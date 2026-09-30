package com.example.minshuku.service;

import com.example.minshuku.config.PublicBookingProperties;
import com.example.minshuku.domain.PublicBookingRequest;
import com.example.minshuku.domain.Reservation;
import com.example.minshuku.domain.ReservationGuest;
import com.example.minshuku.domain.Room;
import com.example.minshuku.domain.RoomPriceRule;
import com.example.minshuku.mapper.PublicBookingMapper;
import com.example.minshuku.mapper.ReservationFinanceMapper;
import com.example.minshuku.mapper.ReservationGuestMapper;
import com.example.minshuku.mapper.ReservationMapper;
import com.example.minshuku.mapper.RoomMapper;
import com.example.minshuku.mapper.RoomPriceRuleMapper;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 客向け複数客室予約の見積、24時間保留、確認、拒否、取消、期限切れを管理する。 */
@Service
public class PublicBookingService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern REQUEST_NO_PATTERN = Pattern.compile("^BR\\d{8,16}$");
    private static final Set<String> LOCALES = Set.of("ja", "zh", "en");
    private static final int MAX_ROOMS = 10;
    private static final int MAX_TOTAL_GUESTS = 30;

    private final PublicBookingMapper publicBookingMapper;
    private final ReservationMapper reservationMapper;
    private final ReservationGuestMapper guestMapper;
    private final ReservationFinanceMapper financeMapper;
    private final RoomMapper roomMapper;
    private final RoomPriceRuleMapper priceRuleMapper;
    private final CustomerService customerService;
    private final PublicBookingProperties properties;
    private final PublicBookingLookupRateLimiter lookupRateLimiter;
    private final ApplicationEventPublisher eventPublisher;
    private final SecureRandom secureRandom = new SecureRandom();
    private final ZoneId zoneId;

    public PublicBookingService(
            PublicBookingMapper publicBookingMapper,
            ReservationMapper reservationMapper,
            ReservationGuestMapper guestMapper,
            ReservationFinanceMapper financeMapper,
            RoomMapper roomMapper,
            RoomPriceRuleMapper priceRuleMapper,
            CustomerService customerService,
            PublicBookingProperties properties,
            PublicBookingLookupRateLimiter lookupRateLimiter,
            ApplicationEventPublisher eventPublisher,
            @Value("${app.time-zone:Asia/Tokyo}") String timeZone) {
        this.publicBookingMapper = publicBookingMapper;
        this.reservationMapper = reservationMapper;
        this.guestMapper = guestMapper;
        this.financeMapper = financeMapper;
        this.roomMapper = roomMapper;
        this.priceRuleMapper = priceRuleMapper;
        this.customerService = customerService;
        this.properties = properties;
        this.lookupRateLimiter = lookupRateLimiter;
        this.eventPublisher = eventPublisher;
        this.zoneId = ZoneId.of(timeZone);
    }

    @Transactional(readOnly = true)
    public PublicConfiguration configuration() {
        return new PublicConfiguration(properties.isTurnstileRequired(), properties.getTurnstileSiteKey(),
                properties.getHoldHours(), properties.getCancellationCutoffHours());
    }

    @Transactional(readOnly = true)
    public List<RoomOption> availableRooms(LocalDate checkInDate, LocalDate checkOutDate) {
        validateDates(checkInDate, checkOutDate);
        return roomMapper.findAvailableForStay(checkInDate, checkOutDate, 1, reservationMapper.currentDate())
                .stream()
                .map(room -> new RoomOption(room.getId(), room.getRoomNumber(), room.getRoomName(),
                        room.getRoomType(), room.getCapacity(), room.getPrivateBath()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Quote quote(QuoteRequest request) {
        validateSelections(request.checkInDate(), request.checkOutDate(), request.rooms(), false);
        List<RoomQuote> roomQuotes = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (RoomSelection selection : request.rooms()) {
            Room room = roomMapper.findById(selection.roomId());
            validateRoom(selection, room);
            ensureAvailable(selection.roomId(), request.checkInDate(), request.checkOutDate());
            RoomQuote quote = calculateRoomQuote(room, selection.guestCount(), request.checkInDate(),
                    request.checkOutDate());
            roomQuotes.add(quote);
            total = total.add(quote.totalAmount());
        }
        return new Quote(request.checkInDate(), request.checkOutDate(), roomQuotes, total);
    }

    @Transactional
    public CreateResult create(CreateRequest request) {
        validateCreate(request);
        List<RoomSelection> selections = request.rooms().stream()
                .sorted(Comparator.comparing(RoomSelection::roomId))
                .toList();
        OffsetDateTime now = OffsetDateTime.now(zoneId);
        String rawToken = newCancelToken();

        PublicBookingRequest booking = new PublicBookingRequest();
        booking.setRequestNo(String.format("BR%08d", publicBookingMapper.nextRequestSequence()));
        booking.setLocale(request.language());
        booking.setStatus("pending");
        booking.setCheckInDate(request.checkInDate());
        booking.setCheckOutDate(request.checkOutDate());
        booking.setLeadName(clean(request.leadName()));
        booking.setLeadEmail(clean(request.email()).toLowerCase());
        booking.setLeadPhone(clean(request.phone()));
        booking.setCountry(clean(request.country()));
        booking.setGuestCount(selections.stream().mapToInt(RoomSelection::guestCount).sum());
        booking.setNotes(trimToNull(request.notes()));
        booking.setCancelTokenHash(hashToken(rawToken));
        booking.setHoldExpiresAt(now.plusHours(properties.getHoldHours()));

        List<LockedSelection> locked = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (RoomSelection selection : selections) {
            Room room = roomMapper.findByIdForUpdate(selection.roomId());
            validateRoom(selection, room);
            ensureAvailable(selection.roomId(), request.checkInDate(), request.checkOutDate());
            RoomQuote roomQuote = calculateRoomQuote(room, selection.guestCount(), request.checkInDate(),
                    request.checkOutDate());
            locked.add(new LockedSelection(selection, room, roomQuote));
            total = total.add(roomQuote.totalAmount());
        }
        booking.setTotalAmount(total);
        publicBookingMapper.insert(booking);

        for (LockedSelection lockedSelection : locked) {
            insertPendingReservation(booking, request, lockedSelection);
        }
        String cancellationUrl = cancellationUrl(rawToken);
        eventPublisher.publishEvent(new BookingNotificationEvent(booking, "pending", cancellationUrl));
        return new CreateResult(booking.getRequestNo(), booking.getStatus(), booking.getHoldExpiresAt(),
                booking.getTotalAmount(), cancellationUrl);
    }

    @Transactional(readOnly = true)
    public List<AdminBooking> pendingRequests() {
        return publicBookingMapper.findPending().stream().map(this::toAdminBooking).toList();
    }

    @Transactional(readOnly = true)
    public BookingLookupView lookup(BookingLookupRequest lookupRequest, String remoteSource) {
        PublicBookingRequest request = authenticateLookup(
                lookupRequest == null ? null : lookupRequest.requestNo(),
                lookupRequest == null ? null : lookupRequest.email(),
                remoteSource,
                false);
        return toBookingLookupView(request);
    }

    @Transactional
    public void confirm(Integer id) {
        PublicBookingRequest request = requirePending(id);
        if (!request.getHoldExpiresAt().isAfter(OffsetDateTime.now(zoneId))) {
            expireOne(request);
            throw new IllegalArgumentException("保留期限已过，房间已经释放。");
        }
        Reservation customerSource = new Reservation();
        customerSource.setGuestName(request.getLeadName());
        customerSource.setGuestEmail(request.getLeadEmail());
        customerSource.setGuestPhone(request.getLeadPhone());
        customerService.resolveForReservation(customerSource);
        publicBookingMapper.linkCustomer(request.getId(), customerSource.getCustomerId());
        List<Reservation> reservations = publicBookingMapper.findReservations(request.getId());
        if (reservations.isEmpty()) {
            throw new InventoryConflictException("预约明细不存在，请刷新后重试。");
        }
        reservations.stream().map(Reservation::getRoomId).distinct().sorted()
                .forEach(roomMapper::findByIdForUpdate);
        for (Reservation reservation : reservations) {
            if (roomMapper.countOverlappingInventoryBlocks(
                    reservation.getRoomId(), reservation.getCheckInDate(), reservation.getCheckOutDate()) > 0) {
                throw new InventoryConflictException("预约房间已停售或进入维修，请重新处理申请。");
            }
        }
        if (publicBookingMapper.updateReservationsStatus(request.getId(), "pending", "booked") != reservations.size()) {
            throw new InventoryConflictException("预约明细状态已经变化，请刷新后重试。");
        }
        for (Reservation reservation : reservations) {
            financeMapper.insertEmpty(reservation.getId());
        }
        updateRequestStatus(request, "pending", "confirmed", null);
        publish(request, "confirmed");
    }

    @Transactional
    public void reject(Integer id, String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new IllegalArgumentException("拒绝预约时必须填写原因。");
        }
        PublicBookingRequest request = requirePending(id);
        List<Reservation> reservations = publicBookingMapper.findReservations(request.getId());
        reservations.stream().map(Reservation::getRoomId).distinct().sorted()
                .forEach(roomMapper::findByIdForUpdate);
        if (publicBookingMapper.updateReservationsStatus(request.getId(), "pending", "cancelled") != reservations
                .size()) {
            throw new InventoryConflictException("预约明细状态已经变化，请刷新后重试。");
        }
        updateRequestStatus(request, "pending", "rejected", clean(reason));
        request.setRejectionReason(clean(reason));
        publish(request, "rejected");
    }

    @Transactional
    public CancellationView cancellation(String rawToken) {
        PublicBookingRequest request = requireByToken(rawToken);
        boolean cancellable = Set.of("pending", "confirmed").contains(request.getStatus())
                && isBeforeCancellationCutoff(request);
        return new CancellationView(request.getRequestNo(), request.getStatus(), request.getCheckInDate(),
                request.getCheckOutDate(), request.getTotalAmount(), cancellable,
                properties.getCancellationCutoffHours());
    }

    @Transactional
    public void cancel(String rawToken, String reason, boolean confirmed) {
        PublicBookingRequest request = requireByToken(rawToken);
        cancelAuthenticated(request, reason, confirmed);
    }

    @Transactional
    public BookingLookupView cancelByLookup(BookingLookupCancelRequest cancelRequest, String remoteSource) {
        PublicBookingRequest request = authenticateLookup(
                cancelRequest == null ? null : cancelRequest.requestNo(),
                cancelRequest == null ? null : cancelRequest.email(),
                remoteSource,
                true);
        cancelAuthenticated(
                request,
                cancelRequest == null ? null : cancelRequest.reason(),
                cancelRequest != null && cancelRequest.confirmed());
        return toBookingLookupView(request);
    }

    private void cancelAuthenticated(PublicBookingRequest request, String reason, boolean confirmed) {
        if (!confirmed) {
            throw new IllegalArgumentException("请在取消前完成最终确认。");
        }
        requireText(reason, 1000, "取消预约时必须填写原因。");
        if (!Set.of("pending", "confirmed").contains(request.getStatus())) {
            throw new IllegalArgumentException("该预约目前无法取消。");
        }
        if (!isBeforeCancellationCutoff(request)) {
            throw new IllegalArgumentException("已超过入住前" + properties.getCancellationCutoffHours()
                    + "小时的自助取消期限，请直接联系住宿方。");
        }
        String reservationStatus = "pending".equals(request.getStatus()) ? "pending" : "booked";
        publicBookingMapper.updateReservationsStatus(request.getId(), reservationStatus, "cancelled");
        String cancellationReason = clean(reason);
        updateRequestStatus(request, request.getStatus(), "cancelled", cancellationReason);
        request.setCancellationReason(cancellationReason);
        publish(request, "cancelled");
    }

    @Transactional
    public int expirePending() {
        List<PublicBookingRequest> expired = publicBookingMapper
                .findExpiredPendingForUpdate(OffsetDateTime.now(zoneId));
        for (PublicBookingRequest request : expired) {
            expireOne(request);
        }
        return expired.size();
    }

    private void expireOne(PublicBookingRequest request) {
        publicBookingMapper.updateReservationsStatus(request.getId(), "pending", "cancelled");
        updateRequestStatus(request, "pending", "expired", null);
        publish(request, "expired");
    }

    private void insertPendingReservation(PublicBookingRequest booking, CreateRequest request,
            LockedSelection lockedSelection) {
        RoomSelection selection = lockedSelection.selection();
        GuestInput leadGuest = selection.guests().get(0);
        Reservation reservation = new Reservation();
        reservation.setReservationNo(String.format("R%06d", reservationMapper.nextReservationSequence()));
        reservation.setRoomId(selection.roomId());
        reservation.setBookingRequestId(booking.getId());
        reservation.setCheckInDate(request.checkInDate());
        reservation.setCheckOutDate(request.checkOutDate());
        reservation.setGuestName(clean(leadGuest.name()));
        reservation.setGuestAge(leadGuest.age());
        reservation.setGuestCategory(leadGuest.category());
        reservation.setGuestPhone(booking.getLeadPhone());
        reservation.setGuestEmail(booking.getLeadEmail());
        reservation.setGuestCount(selection.guestCount());
        reservation.setReservationForm("公式Web");
        reservation.setPaymentStatus("unpaid");
        reservation.setReservationStatus("pending");
        reservation.setTotalAmount(lockedSelection.quote().totalAmount());
        reservation.setNote(
                booking.getRequestNo() + (StringUtils.hasText(booking.getNotes()) ? " / " + booking.getNotes() : ""));
        reservationMapper.insert(reservation);
        for (int index = 1; index < selection.guests().size(); index += 1) {
            GuestInput input = selection.guests().get(index);
            ReservationGuest guest = new ReservationGuest();
            guest.setReservationId(reservation.getId());
            guest.setGuestName(clean(input.name()));
            guest.setGuestAge(input.age());
            guest.setGuestCategory(input.category());
            guestMapper.insert(guest);
        }
    }

    private RoomQuote calculateRoomQuote(Room room, int guestCount, LocalDate checkInDate, LocalDate checkOutDate) {
        List<NightlyRate> nights = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (LocalDate date = checkInDate; date.isBefore(checkOutDate); date = date.plusDays(1)) {
            RoomPriceRule rule = priceRuleMapper.findBestRule(room.getId(), date);
            BigDecimal perPerson = rule == null ? room.getBasePricePerPerson() : rule.getPricePerPerson();
            BigDecimal nightTotal = perPerson.multiply(BigDecimal.valueOf(guestCount));
            nights.add(new NightlyRate(date, perPerson, guestCount, nightTotal,
                    rule == null ? "base" : rule.getRuleName()));
            total = total.add(nightTotal);
        }
        return new RoomQuote(room.getId(), room.getRoomNumber(), room.getRoomName(), room.getCapacity(),
                guestCount, nights, total);
    }

    private void validateCreate(CreateRequest request) {
        validateSelections(request.checkInDate(), request.checkOutDate(), request.rooms(), true);
        requireText(request.leadName(), 100, "代表者姓名不能为空。");
        requireText(request.email(), 255, "邮箱不能为空。");
        if (!EMAIL_PATTERN.matcher(request.email()).matches()) {
            throw new IllegalArgumentException("邮箱格式不正确。");
        }
        requireText(request.phone(), 50, "电话号码不能为空。");
        requireText(request.country(), 100, "国家或地区不能为空。");
        if (!LOCALES.contains(request.language())) {
            throw new IllegalArgumentException("语言设置不正确。");
        }
        if (!request.consent()) {
            throw new IllegalArgumentException("必须同意预约申请的数据使用说明。");
        }
        if (request.notes() != null && request.notes().length() > 1000) {
            throw new IllegalArgumentException("备注不能超过1000个字符。");
        }
    }

    private void validateSelections(LocalDate checkInDate, LocalDate checkOutDate, List<RoomSelection> rooms,
            boolean requireGuests) {
        validateDates(checkInDate, checkOutDate);
        if (rooms == null || rooms.isEmpty() || rooms.size() > MAX_ROOMS) {
            throw new IllegalArgumentException("请选择1至10个房间。");
        }
        Set<Integer> ids = new HashSet<>();
        int totalGuests = 0;
        for (RoomSelection room : rooms) {
            if (room == null || room.roomId() == null || !ids.add(room.roomId())) {
                throw new IllegalArgumentException("不能重复选择同一个房间。");
            }
            if (room.guestCount() < 1) {
                throw new IllegalArgumentException("每个房间至少需要1名入住者。");
            }
            totalGuests += room.guestCount();
            if (requireGuests) {
                validateGuests(room);
            }
        }
        if (totalGuests > MAX_TOTAL_GUESTS) {
            throw new IllegalArgumentException("一次申请最多可登记30名入住者。");
        }
    }

    private void validateGuests(RoomSelection room) {
        if (room.guests() == null || room.guests().size() != room.guestCount()) {
            throw new IllegalArgumentException("每个房间的入住者人数必须与填写人数一致。");
        }
        for (GuestInput guest : room.guests()) {
            requireText(guest.name(), 100, "所有入住者都必须填写姓名。");
            if (!Set.of("adult", "child").contains(guest.category())) {
                throw new IllegalArgumentException("请选择入住者是成人还是儿童。");
            }
            if ("child".equals(guest.category()) && (guest.age() == null || guest.age() < 0 || guest.age() > 17)) {
                throw new IllegalArgumentException("儿童必须填写0至17岁的年龄。");
            }
            if ("adult".equals(guest.category()) && guest.age() != null && (guest.age() < 18 || guest.age() > 130)) {
                throw new IllegalArgumentException("成人年龄必须为18至130岁，或留空。");
            }
        }
    }

    private void validateDates(LocalDate checkInDate, LocalDate checkOutDate) {
        LocalDate today = reservationMapper.currentDate();
        if (checkInDate == null || checkOutDate == null || checkInDate.isBefore(today)) {
            throw new IllegalArgumentException("入住日期必须是今天或之后。");
        }
        if (!checkOutDate.isAfter(checkInDate)) {
            throw new IllegalArgumentException("退房日期必须晚于入住日期。");
        }
        if (checkOutDate.isAfter(checkInDate.plusDays(30))) {
            throw new IllegalArgumentException("一次预约最多可住宿30晚。");
        }
    }

    private void validateRoom(RoomSelection selection, Room room) {
        if (room == null || !Boolean.TRUE.equals(room.getActive())) {
            throw new IllegalArgumentException("所选房间已经不可预约。");
        }
        if (selection.guestCount() > room.getCapacity()) {
            throw new IllegalArgumentException(room.getRoomNumber() + "号房超过可入住人数。");
        }
    }

    private void ensureAvailable(Integer roomId, LocalDate checkInDate, LocalDate checkOutDate) {
        if (reservationMapper.countOverlapping(roomId, checkInDate, checkOutDate) > 0) {
            throw new IllegalArgumentException("所选房间刚刚被其他客人预约，请重新选择。");
        }
        if (roomMapper.countOverlappingInventoryBlocks(roomId, checkInDate, checkOutDate) > 0) {
            throw new InventoryConflictException("所选房间在该期间暂停销售或维修中，请重新选择。");
        }
    }

    private PublicBookingRequest requirePending(Integer id) {
        PublicBookingRequest request = publicBookingMapper.findByIdForUpdate(id);
        if (request == null || !"pending".equals(request.getStatus())) {
            throw new IllegalArgumentException("找不到等待确认的预约申请。");
        }
        return request;
    }

    private PublicBookingRequest requireByToken(String rawToken) {
        if (!StringUtils.hasText(rawToken) || rawToken.length() > 200) {
            throw new IllegalArgumentException("取消链接无效。");
        }
        PublicBookingRequest request = publicBookingMapper.findByTokenHashForUpdate(hashToken(rawToken));
        if (request == null) {
            throw new IllegalArgumentException("取消链接无效。");
        }
        return request;
    }

    private PublicBookingRequest authenticateLookup(
            String requestNo,
            String email,
            String remoteSource,
            boolean forUpdate) {
        String normalizedRequestNo = normalizeRequestNo(requestNo);
        String normalizedEmail = normalizeEmail(email);
        return lookupRateLimiter.authenticate(remoteSource, () -> {
            if (normalizedRequestNo == null || normalizedEmail == null) {
                return null;
            }
            return forUpdate
                    ? publicBookingMapper.findByCredentialsForUpdate(normalizedRequestNo, normalizedEmail)
                    : publicBookingMapper.findByCredentials(normalizedRequestNo, normalizedEmail);
        });
    }

    private String normalizeRequestNo(String requestNo) {
        if (!StringUtils.hasText(requestNo)) {
            return null;
        }
        String normalized = requestNo.trim().toUpperCase(Locale.ROOT);
        return REQUEST_NO_PATTERN.matcher(normalized).matches() ? normalized : null;
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email) || email.length() > 255) {
            return null;
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        return EMAIL_PATTERN.matcher(normalized).matches() ? normalized : null;
    }

    private boolean isBeforeCancellationCutoff(PublicBookingRequest request) {
        OffsetDateTime cutoff = request.getCheckInDate().atStartOfDay(zoneId).toOffsetDateTime()
                .minusHours(properties.getCancellationCutoffHours());
        return OffsetDateTime.now(zoneId).isBefore(cutoff);
    }

    private void updateRequestStatus(PublicBookingRequest request, String expected, String status, String reason) {
        OffsetDateTime now = OffsetDateTime.now(zoneId);
        if (publicBookingMapper.updateStatus(request.getId(), expected, status, reason, now) == 0) {
            throw new IllegalArgumentException("预约状态已经变化，请刷新后重试。");
        }
        request.setStatus(status);
        if ("rejected".equals(status)) {
            request.setRejectionReason(reason);
        }
        if ("cancelled".equals(status)) {
            request.setCancellationReason(reason);
        }
    }

    private AdminBooking toAdminBooking(PublicBookingRequest request) {
        return new AdminBooking(request, publicBookingMapper.findReservations(request.getId()));
    }

    private BookingLookupView toBookingLookupView(PublicBookingRequest request) {
        List<BookingLookupRoom> rooms = publicBookingMapper.findReservations(request.getId()).stream()
                .map(reservation -> new BookingLookupRoom(
                        reservation.getRoomNumber(),
                        reservation.getRoomName(),
                        reservation.getGuestCount(),
                        reservation.getTotalAmount()))
                .toList();
        boolean cancellable = Set.of("pending", "confirmed").contains(request.getStatus())
                && isBeforeCancellationCutoff(request);
        return new BookingLookupView(
                request.getRequestNo(),
                request.getStatus(),
                request.getCheckInDate(),
                request.getCheckOutDate(),
                request.getGuestCount(),
                request.getTotalAmount(),
                request.getHoldExpiresAt(),
                request.getRejectionReason(),
                request.getCancellationReason(),
                cancellable,
                properties.getCancellationCutoffHours(),
                rooms);
    }

    private void publish(PublicBookingRequest request, String type) {
        eventPublisher.publishEvent(new BookingNotificationEvent(request, type, null));
    }

    private String cancellationUrl(String rawToken) {
        return properties.getPublicBaseUrl().replaceAll("/$", "") + "/stay/cancel?token=" + rawToken;
    }

    private String newCancelToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable.", ex);
        }
    }

    private void requireText(String value, int maxLength, String message) {
        if (!StringUtils.hasText(value) || value.length() > maxLength) {
            throw new IllegalArgumentException(message);
        }
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }

    private String trimToNull(String value) {
        String cleaned = clean(value);
        return StringUtils.hasText(cleaned) ? cleaned : null;
    }

    public record PublicConfiguration(boolean turnstileRequired, String turnstileSiteKey, int holdHours,
            int cancellationCutoffHours) {
    }
    public record RoomOption(Integer id, String roomNumber, String roomName, String roomType, int capacity,
            boolean privateBath) {
    }
    public record GuestInput(String name, String category, Integer age) {
    }
    public record RoomSelection(Integer roomId, int guestCount, List<GuestInput> guests) {
    }
    public record QuoteRequest(LocalDate checkInDate, LocalDate checkOutDate, List<RoomSelection> rooms) {
    }
    public record CreateRequest(LocalDate checkInDate, LocalDate checkOutDate, List<RoomSelection> rooms,
            String leadName, String email, String phone, String country, String notes, String language,
            boolean consent, String turnstileToken) {
    }
    public record NightlyRate(LocalDate date, BigDecimal pricePerPerson, int guestCount, BigDecimal amount,
            String ruleName) {
    }
    public record RoomQuote(Integer roomId, String roomNumber, String roomName, int capacity, int guestCount,
            List<NightlyRate> nights, BigDecimal totalAmount) {
    }
    public record Quote(LocalDate checkInDate, LocalDate checkOutDate, List<RoomQuote> rooms,
            BigDecimal totalAmount) {
    }
    public record CreateResult(String requestNo, String status, OffsetDateTime holdExpiresAt, BigDecimal totalAmount,
            String cancellationUrl) {
    }
    public record AdminBooking(PublicBookingRequest request, List<Reservation> rooms) {
    }
    public record BookingLookupRequest(String requestNo, String email) {
    }
    public record BookingLookupCancelRequest(String requestNo, String email, String reason, boolean confirmed) {
    }
    public record BookingLookupRoom(String roomNumber, String roomName, int guestCount, BigDecimal totalAmount) {
    }
    public record BookingLookupView(
            String requestNo,
            String status,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            int guestCount,
            BigDecimal totalAmount,
            OffsetDateTime holdExpiresAt,
            String rejectionReason,
            String cancellationReason,
            boolean cancellable,
            int cancellationCutoffHours,
            List<BookingLookupRoom> rooms) {
    }
    public record CancellationView(String requestNo, String status, LocalDate checkInDate, LocalDate checkOutDate,
            BigDecimal totalAmount, boolean cancellable, int cancellationCutoffHours) {
    }
    private record LockedSelection(RoomSelection selection, Room room, RoomQuote quote) {
    }
}
