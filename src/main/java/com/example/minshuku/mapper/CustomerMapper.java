package com.example.minshuku.mapper;

import com.example.minshuku.domain.Customer;
import com.example.minshuku.domain.Reservation;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 顧客マスタと宿泊履歴を扱う MyBatis Mapper。 */
@Mapper
public interface CustomerMapper {
    long nextCustomerSequence();

    List<Customer> search(@Param("query") String query, @Param("limit") int limit);

    Customer findById(@Param("id") Integer id);

    Customer findExact(@Param("name") String name, @Param("phone") String phone, @Param("email") String email);

    int insert(Customer customer);

    int update(Customer customer);

    List<Reservation> findStayHistory(@Param("customerId") Integer customerId);
}
