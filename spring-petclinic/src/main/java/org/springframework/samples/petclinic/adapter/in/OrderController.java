package org.springframework.samples.petclinic.adapter.in;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.samples.petclinic.domain.*;
import org.springframework.samples.petclinic.adapter.out.OrderRepository;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

	private final OrderRepository repository;

	public OrderController(OrderRepository repository) {
		this.repository = repository;
	}

	@PostMapping
	public Order create(@RequestParam Long petId, @RequestParam String dateTime) {

		Order order = Order.create(petId, LocalDateTime.parse(dateTime));
		return repository.save(order);
	}

	@PatchMapping("/{id}/status")
	public ResponseEntity<?> changeStatus(@PathVariable Integer id, @RequestParam String status) {

		Order order = repository.findById(id).orElse(null);
		if (order == null)
			return ResponseEntity.notFound().build();

		try {
			order.changeStatus(OrderStatus.valueOf(status));
			repository.save(order);
		}
		catch (Exception e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}

		return ResponseEntity.ok(order);
	}

}
