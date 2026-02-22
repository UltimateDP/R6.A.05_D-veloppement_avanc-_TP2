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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.samples.petclinic.adapter.out.AppointmentRepository;
import org.springframework.samples.petclinic.adapter.out.OwnerRepository;
import org.springframework.samples.petclinic.domain.Appointment;
import org.springframework.samples.petclinic.domain.AppointmentStatus;
import org.springframework.samples.petclinic.domain.Owner;

/**
 * Classe de tests unitaires pour {@link AppointmentService}.
 *
 * Cette classe teste la couche APPLICATION (use cases) qui orchestre la logique métier
 * entre le domaine et la persistance.
 *
 * <p>
 * Type de tests : Tests UNITAIRES avec MOCKS
 * <ul>
 * <li>On utilise Mockito pour simuler (mock) les repositories</li>
 * <li>On teste uniquement la logique du service, pas la BDD</li>
 * <li>Tests rapides (pas de Spring Context, pas de BDD)</li>
 * </ul>
 *
 * <p>
 * Responsabilités testées du service :
 * <ul>
 * <li>Validation de l'existence de l'owner</li>
 * <li>Initialisation du statut CREATED</li>
 * <li>Vérification des droits (ownership)</li>
 * <li>Orchestration des appels domaine/repository</li>
 * </ul>
 *
 * <p>
 * Architecture hexagonale : Ces tests sont dans la couche APPLICATION car ils testent les
 * cas d'usage (use cases).
 *
 * @author Naiyma
 */
@ExtendWith(MockitoExtension.class)
class AppointmentServiceTests {

	@Mock
	private AppointmentRepository appointments;

	@Mock
	private OwnerRepository owners;

	@InjectMocks
	private AppointmentService service;

	/**
	 * Teste la création d'un appointment avec initialisation correcte.
	 *
	 * <p>
	 * Ce test vérifie que createAppointment() :
	 * <ol>
	 * <li>Récupère l'owner depuis la BDD (via le repository)</li>
	 * <li>Associe l'owner au rendez-vous</li>
	 * <li>Initialise le statut à CREATED</li>
	 * <li>Sauvegarde le rendez-vous</li>
	 * </ol>
	 *
	 * <p>
	 * Méthode de test :
	 * <ul>
	 * <li>given() : On simule (mock) que l'owner existe dans la BDD</li>
	 * <li>given() : On simule que save() retourne l'objet sauvé</li>
	 * <li>when : On appelle createAppointment()</li>
	 * <li>then : On vérifie owner et statut</li>
	 * </ul>
	 *
	 * <p>
	 * Pattern BDD (Behavior-Driven Development) : given/when/then
	 *
	 * @author Naiyma
	 */
	@Test
	void createAppointmentSetsOwnerAndCreatedStatus() {
		// GIVEN : Un owner existant dans la BDD
		Owner owner = new Owner();
		owner.setId(1);
		Appointment appointment = new Appointment();
		given(this.owners.findById(1)).willReturn(Optional.of(owner));

		// GIVEN : Le repository sauvegarde et retourne l'appointment
		given(this.appointments.save(any(Appointment.class))).willAnswer(invocation -> invocation.getArgument(0));

		// WHEN : On crée un rendez-vous pour cet owner
		Appointment saved = this.service.createAppointment(1, appointment);

		// THEN : L'owner est associé et le statut est CREATED
		assertThat(saved.getOwner()).isEqualTo(owner);
		assertThat(saved.getStatus()).isEqualTo(AppointmentStatus.CREATED);
	}

	/**
	 * Teste le rejet de création si l'owner n'existe pas.
	 *
	 * <p>
	 * Ce test vérifie la VALIDATION : on ne peut pas créer un rendez-vous pour un owner
	 * qui n'existe pas dans la base de données.
	 *
	 * <p>
	 * Règle métier : Tout appointment doit avoir un owner valide.
	 *
	 * <p>
	 * Méthode de test :
	 * <ul>
	 * <li>given() : On simule que l'owner ID=99 n'existe PAS (Optional.empty)</li>
	 * <li>when : On essaie de créer un appointment pour cet owner</li>
	 * <li>then : Le service lève une IllegalArgumentException</li>
	 * </ul>
	 *
	 * <p>
	 * Pourquoi IllegalArgumentException ? Parce que l'ID fourni est invalide.
	 *
	 * @author Naiyma
	 */
	@Test
	void createAppointmentRejectsMissingOwner() {
		// GIVEN : Un owner qui n'existe PAS dans la BDD
		Appointment appointment = new Appointment();
		given(this.owners.findById(99)).willReturn(Optional.empty());

		// WHEN : On essaie de créer un rendez-vous pour cet owner
		// THEN : Une exception doit être levée
		assertThatThrownBy(() -> this.service.createAppointment(99, appointment))
			.isInstanceOf(IllegalArgumentException.class);
	}

	/**
	 * Teste le rejet de mise à jour si l'owner ne correspond pas.
	 *
	 * <p>
	 * Ce test vérifie le CONTRÔLE D'ACCÈS (ownership) : Seul le propriétaire du
	 * rendez-vous peut modifier son statut.
	 *
	 * <p>
	 * Scénario : L'appointment appartient à l'owner ID=1, mais on essaie de le modifier
	 * avec l'owner ID=2.
	 *
	 * <p>
	 * Règle métier : Sécurité - un owner ne peut pas modifier les rendez-vous d'un autre
	 * owner.
	 *
	 * <p>
	 * Méthode de test :
	 * <ul>
	 * <li>given() : Un appointment appartenant à owner ID=1</li>
	 * <li>when : Owner ID=2 essaie de modifier le statut</li>
	 * <li>then : IllegalArgumentException (accès refusé)</li>
	 * </ul>
	 *
	 * @author Naiyma
	 */
	@Test
	void updateStatusRejectsWrongOwner() {
		// GIVEN : Un rendez-vous appartenant à l'owner ID=1
		Owner owner = new Owner();
		owner.setId(1);
		Appointment appointment = new Appointment();
		appointment.setOwner(owner);
		appointment.setStatus(AppointmentStatus.CREATED);
		given(this.appointments.findById(10)).willReturn(Optional.of(appointment));

		// WHEN : L'owner ID=2 essaie de modifier le statut
		// THEN : Une exception doit être levée (pas le bon owner)
		assertThatThrownBy(() -> this.service.updateStatus(2, 10, AppointmentStatus.CONFIRMED))
			.isInstanceOf(IllegalArgumentException.class);
	}

	/**
	 * Teste la mise à jour de statut avec application des règles métier.
	 *
	 * <p>
	 * Ce test vérifie que updateStatus() :
	 * <ol>
	 * <li>Vérifie l'ownership (le bon owner)</li>
	 * <li>Délègue au domaine (appointment.changeStatus)</li>
	 * <li>Sauvegarde les changements</li>
	 * </ol>
	 *
	 * <p>
	 * Orchestration : Le service coordonne mais ne contient PAS la logique de transition.
	 * C'est le domaine (Appointment.changeStatus) qui contient les règles de la state
	 * machine.
	 *
	 * <p>
	 * Méthode de test :
	 * <ul>
	 * <li>given() : Un appointment CREATED appartenant au bon owner</li>
	 * <li>when : On met à jour vers CONFIRMED avec le bon owner ID</li>
	 * <li>then : Le statut change avec succès</li>
	 * </ul>
	 *
	 * <p>
	 * Principe : Separation of Concerns (séparation des responsabilités).
	 *
	 * @author Naiyma
	 */
	@Test
	void updateStatusAppliesBusinessRules() {
		// GIVEN : Un rendez-vous CREATED appartenant à l'owner ID=1
		Owner owner = new Owner();
		owner.setId(1);
		Appointment appointment = new Appointment();
		appointment.setOwner(owner);
		appointment.setStatus(AppointmentStatus.CREATED);
		given(this.appointments.findById(10)).willReturn(Optional.of(appointment));
		given(this.appointments.save(any(Appointment.class))).willAnswer(invocation -> invocation.getArgument(0));

		// WHEN : Le bon owner confirme le rendez-vous
		Appointment updated = this.service.updateStatus(1, 10, AppointmentStatus.CONFIRMED);

		// THEN : Le statut est bien CONFIRMED
		assertThat(updated.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
	}

}
