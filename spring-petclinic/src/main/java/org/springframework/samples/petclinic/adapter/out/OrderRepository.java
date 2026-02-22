package org.springframework.samples.petclinic.adapter.out;

import org.springframework.data.repository.CrudRepository;
import org.springframework.samples.petclinic.domain.Order;

public interface OrderRepository extends CrudRepository<Order, Integer> {

}
