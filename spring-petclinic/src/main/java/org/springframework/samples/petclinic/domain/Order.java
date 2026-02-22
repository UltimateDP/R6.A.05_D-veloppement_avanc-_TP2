package org.springframework.samples.petclinic.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

	private Long petId;

	private LocalDateTime dateTime;

	@Enumerated(EnumType.STRING)
	private OrderStatus status;

	public static Order create(Long petId, LocalDateTime dateTime) {
		Order order = new Order();
		order.petId = petId;
		order.dateTime = dateTime;
		order.status = OrderStatus.CREATED;
		return order;
	}

	public void changeStatus(OrderStatus target) {
		switch (target) {
			case CONFIRMED -> {
				if (status != OrderStatus.CREATED)
					throw new IllegalStateException("CONFIRMED seulement si CREATED");
			}
			case DONE -> {
				if (status != OrderStatus.CONFIRMED)
					throw new IllegalStateException("DONE seulement si CONFIRMED");
			}
			case CANCELLED -> {
				if (status == OrderStatus.DONE)
					throw new IllegalStateException("CANCELLED impossible depuis DONE");
			}
			case CREATED -> throw new IllegalStateException("Retour à CREATED interdit");
		}
		this.status = target;
	}

	public Long getPetId() {
		return petId;
	}

	public void setPetId(Long petId) {
		this.petId = petId;
	}

	public LocalDateTime getDateTime() {
		return dateTime;
	}

	public void setDateTime(LocalDateTime dateTime) {
		this.dateTime = dateTime;
	}

	public OrderStatus getStatus() {
		return status;
	}

	public void setStatus(OrderStatus status) {
		this.status = status;
	}

}
