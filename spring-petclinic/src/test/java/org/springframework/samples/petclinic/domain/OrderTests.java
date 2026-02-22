package org.springframework.samples.petclinic.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class OrderTests {

	@Test
	void created_can_confirm() {
		Order order = Order.create(1L, LocalDateTime.now());

		order.changeStatus(OrderStatus.CONFIRMED);

		assertEquals(OrderStatus.CONFIRMED, order.getStatus());
	}

	@Test
	void confirmed_can_done() {
		Order order = Order.create(1L, LocalDateTime.now());
		order.changeStatus(OrderStatus.CONFIRMED);

		order.changeStatus(OrderStatus.DONE);

		assertEquals(OrderStatus.DONE, order.getStatus());
	}

	@Test
	void created_cannot_done_directly() {
		Order order = Order.create(1L, LocalDateTime.now());

		IllegalStateException ex = assertThrows(IllegalStateException.class,
				() -> order.changeStatus(OrderStatus.DONE));

		assertTrue(ex.getMessage().toLowerCase().contains("done"));
	}

	@Test
	void confirmed_only_if_created() {
		Order order = Order.create(1L, LocalDateTime.now());
		order.changeStatus(OrderStatus.CONFIRMED);

		IllegalStateException ex = assertThrows(IllegalStateException.class,
				() -> order.changeStatus(OrderStatus.CONFIRMED));

		assertTrue(ex.getMessage().toLowerCase().contains("confirmed"));
	}

	@Test
	void cancelled_from_created_is_allowed() {
		Order order = Order.create(1L, LocalDateTime.now());

		order.changeStatus(OrderStatus.CANCELLED);

		assertEquals(OrderStatus.CANCELLED, order.getStatus());
	}

	@Test
	void cancelled_from_confirmed_is_allowed() {
		Order order = Order.create(1L, LocalDateTime.now());
		order.changeStatus(OrderStatus.CONFIRMED);

		order.changeStatus(OrderStatus.CANCELLED);

		assertEquals(OrderStatus.CANCELLED, order.getStatus());
	}

	@Test
	void cancelled_from_done_is_forbidden() {
		Order order = Order.create(1L, LocalDateTime.now());
		order.changeStatus(OrderStatus.CONFIRMED);
		order.changeStatus(OrderStatus.DONE);

		IllegalStateException ex = assertThrows(IllegalStateException.class,
				() -> order.changeStatus(OrderStatus.CANCELLED));

		assertTrue(ex.getMessage().toLowerCase().contains("cancel"));
	}

}
