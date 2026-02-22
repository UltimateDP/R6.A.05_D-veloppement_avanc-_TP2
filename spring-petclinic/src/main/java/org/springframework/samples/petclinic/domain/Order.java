package org.springframework.samples.petclinic.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// @author Mathias Verhaeghem-lipson
// Entité représentant une commande de service pour un animal
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

	private Long petId;

	private LocalDateTime dateTime;

	// Statut de la commande, stocké en tant que chaîne dans la base de données
	@Enumerated(EnumType.STRING)
	private OrderStatus status;

	// Création d'une nouvelle commande à partir de l'id de l'animal et de la date/heure
	// souhaitée
	public static Order create(Long petId, LocalDateTime dateTime) {
		Order order = new Order();
		order.petId = petId;
		order.dateTime = dateTime;
		order.status = OrderStatus.CREATED;
		return order;
	}

	// Changement du statut de la commande, avec vérification des transitions autorisées
	public void changeStatus(OrderStatus target) {
		switch (target) {
			// Transition autorisée : CREATED -> CONFIRMED -> DONE
			case CONFIRMED -> {
				if (status != OrderStatus.CREATED)
					throw new IllegalStateException("CONFIRMED seulement si CREATED");
			}
			case DONE -> {
				if (status != OrderStatus.CONFIRMED)
					throw new IllegalStateException("DONE seulement si CONFIRMED");
			}
			// Transition autorisée : CONFIRMED -> CANCELLED ou CREATED -> CANCELLED
			case CANCELLED -> {
				if (status == OrderStatus.DONE)
					throw new IllegalStateException("CANCELLED impossible depuis DONE");
			}
			// Transition interdite : toute transition vers CREATED
			case CREATED -> throw new IllegalStateException("Retour à CREATED interdit");
		}
		this.status = target;
	}

	// Getters et setters
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
