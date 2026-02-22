/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.domain;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Entite de domaine qui represente un rendez-vous d'un proprietaire. Elle contient les
 * donnees (date, reason, status, owner) et applique les regles de transition des statuts
 * au meme endroit.
 *
 * @author Naiyma
 */
@Entity
@Table(name = "appointments")
public class Appointment extends BaseEntity {

	@Column(name = "appointment_date")
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	@NotNull
	private LocalDate date;

	@NotBlank
	private String reason;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	@NotNull
	private AppointmentStatus status;

	@ManyToOne(optional = false)
	@JoinColumn(name = "owner_id")
	private Owner owner;

	public Appointment() {
		this.date = LocalDate.now();
		this.status = AppointmentStatus.CREATED;
	}

	/**
	 * Retourne la date du rendez-vous telle qu'enregistree.
	 * @return date actuelle du rendez-vous
	 */
	public LocalDate getDate() {
		return this.date;
	}

	/**
	 * Modifie la date du rendez-vous.
	 * @param date nouvelle date choisie
	 */
	public void setDate(LocalDate date) {
		this.date = date;
	}

	/**
	 * Retourne le motif (reason) saisi par l'utilisateur.
	 * @return motif du rendez-vous
	 */
	public String getReason() {
		return this.reason;
	}

	/**
	 * Met a jour le motif (reason) du rendez-vous.
	 * @param reason texte court qui explique la demande
	 */
	public void setReason(String reason) {
		this.reason = reason;
	}

	/**
	 * Retourne le statut courant du rendez-vous.
	 * @return statut actuel
	 */
	public AppointmentStatus getStatus() {
		return this.status;
	}

	/**
	 * Fixe le statut sans verifier les regles metier. Utilise au moment de la creation
	 * pour initialiser a CREATED.
	 * @param status statut a appliquer
	 */
	public void setStatus(AppointmentStatus status) {
		this.status = status;
	}

	/**
	 * Retourne le proprietaire (owner) lie au rendez-vous.
	 * @return owner associe
	 */
	public Owner getOwner() {
		return this.owner;
	}

	/**
	 * Lie un proprietaire au rendez-vous.
	 * @param owner proprietaire a associer
	 */
	public void setOwner(Owner owner) {
		this.owner = owner;
	}

	/**
	 * Change le statut en respectant les regles metier. Exemples: CREATED -> CONFIRMED
	 * ok, DONE -> CREATED interdit.
	 * @param newStatus statut cible
	 */
	public void changeStatus(AppointmentStatus newStatus) {
		if (newStatus == null) {
			throw new IllegalArgumentException("Status is required");
		}
		if (newStatus == this.status) {
			return;
		}
		if (!isTransitionAllowed(this.status, newStatus)) {
			throw new IllegalStateException(
					"Cannot change appointment status from " + this.status + " to " + newStatus);
		}
		this.status = newStatus;
	}

	/**
	 * Verifie si la transition from -> to est autorisee.
	 * @param from statut actuel
	 * @param to statut cible
	 * @return true si la transition est valide
	 */
	private boolean isTransitionAllowed(AppointmentStatus from, AppointmentStatus to) {
		if (from == null || to == null) {
			return false;
		}
		return switch (from) {
			case CREATED -> to == AppointmentStatus.CONFIRMED || to == AppointmentStatus.CANCELLED;
			case CONFIRMED -> to == AppointmentStatus.DONE || to == AppointmentStatus.CANCELLED;
			case DONE, CANCELLED -> false;
		};
	}

}
