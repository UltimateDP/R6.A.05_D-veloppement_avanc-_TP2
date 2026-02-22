package org.springframework.samples.petclinic.adapter.out;

import org.springframework.data.repository.CrudRepository;
import org.springframework.samples.petclinic.domain.Order;

// @author Mathias Verhaeghem-lipson
// Repository d'accès aux données pour les commandes
public interface OrderRepository extends CrudRepository<Order, Integer> {

}
