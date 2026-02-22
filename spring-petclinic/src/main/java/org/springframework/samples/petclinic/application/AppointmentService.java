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
package org.springframework.samples.petclinic.application;

import java.util.List;

import org.springframework.samples.petclinic.adapter.out.AppointmentRepository;
import org.springframework.samples.petclinic.adapter.out.OwnerRepository;
import org.springframework.samples.petclinic.domain.Appointment;
import org.springframework.samples.petclinic.domain.AppointmentStatus;
import org.springframework.samples.petclinic.domain.Owner;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * Service applicatif (cas d'usage) pour gerer les rendez-vous. Ici on orchestre: trouver
 * l'owner, verifier les regles, puis sauvegarder.
 *
 * @author Naiyma
 */
@Service
public class AppointmentService {

	private final AppointmentRepository appointments;

	private final OwnerRepository owners;

	public AppointmentService(AppointmentRepository appointments, OwnerRepository owners) {
		this.appointments = appointments;
		this.owners = owners;
	}

	/**
	 * Cree un rendez-vous pour un owner donne. 1) charge l'owner, 2) associe l'owner au
	 * rendez-vous, 3) force le statut a CREATED, 4) sauvegarde en base.
	 * @param ownerId id du proprietaire
	 * @param appointment rendez-vous a creer
	 * @return rendez-vous sauvegarde
	 */
	public Appointment createAppointment(int ownerId, Appointment appointment) {
		Assert.notNull(appointment, "Appointment must not be null");
		Owner owner = this.owners.findById(ownerId)
			.orElseThrow(() -> new IllegalArgumentException("Owner not found with id: " + ownerId));
		appointment.setOwner(owner);
		appointment.setStatus(AppointmentStatus.CREATED);
		return this.appointments.save(appointment);
	}

	/**
	 * Change le statut d'un rendez-vous appartenant a un owner. 1) charge le rendez-vous,
	 * 2) verifie l'owner, 3) applique les regles metier (changeStatus), 4) sauvegarde.
	 * @param ownerId id du proprietaire
	 * @param appointmentId id du rendez-vous
	 * @param status statut cible
	 * @return rendez-vous sauvegarde
	 */
	public Appointment updateStatus(int ownerId, int appointmentId, AppointmentStatus status) {
		Appointment appointment = this.appointments.findById(appointmentId)
			.orElseThrow(() -> new IllegalArgumentException("Appointment not found with id: " + appointmentId));
		if (appointment.getOwner() == null || !appointment.getOwner().getId().equals(ownerId)) {
			throw new IllegalArgumentException("Appointment does not belong to owner: " + ownerId);
		}
		appointment.changeStatus(status);
		return this.appointments.save(appointment);
	}

	/**
	 * Liste tous les rendez-vous d'un owner.
	 * @param ownerId id du proprietaire
	 * @return liste des rendez-vous
	 */
	public List<Appointment> findByOwnerId(int ownerId) {
		return this.appointments.findByOwnerId(ownerId);
	}

}
