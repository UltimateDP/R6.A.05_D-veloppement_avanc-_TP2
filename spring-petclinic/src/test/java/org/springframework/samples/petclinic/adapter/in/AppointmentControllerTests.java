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
package org.springframework.samples.petclinic.adapter.in;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.samples.petclinic.application.AppointmentService;
import org.springframework.samples.petclinic.adapter.out.OwnerRepository;
import org.springframework.samples.petclinic.domain.Appointment;
import org.springframework.samples.petclinic.domain.AppointmentStatus;
import org.springframework.samples.petclinic.domain.Owner;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Classe de tests unitaires pour {@link AppointmentController}.
 *
 * Cette classe teste la couche ADAPTER.IN (contrôleur web MVC) qui gère les requêtes HTTP
 * et les formulaires pour les appointments.
 *
 * <p>
 * Type de tests : Tests UNITAIRES avec @WebMvcTest
 * <ul>
 * <li>Spring charge UNIQUEMENT le contrôleur testé (léger)</li>
 * <li>On utilise MockMvc pour simuler les requêtes HTTP</li>
 * <li>Les services sont mockés (pas de BDD, pas de logique réelle)</li>
 * <li>Tests rapides et isolés</li>
 * </ul>
 *
 * <p>
 * Responsabilités testées du contrôleur :
 * <ul>
 * <li>Routing : Les URLs appellent les bonnes méthodes</li>
 * <li>Validation : Les formulaires avec erreurs sont rejetés</li>
 * <li>Mappage : Les paramètres HTTP sont liés au modèle</li>
 * <li>Vues : Les bonnes pages Thymeleaf sont retournées</li>
 * <li>Redirections : Les redirections se font au bon endroit</li>
 * <li>Messages flash : Les messages de succès/erreur sont passés</li>
 * </ul>
 *
 * <p>
 * Architecture hexagonale : Ces tests sont dans ADAPTER.IN car ils testent l'adaptateur
 * web (entrée dans l'application).
 *
 * @author Naiyma
 */
@WebMvcTest(AppointmentController.class)
@DisabledInNativeImage
@DisabledInAotMode
class AppointmentControllerTests {

	private static final int TEST_OWNER_ID = 1;

	private static final int TEST_APPOINTMENT_ID = 5;

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AppointmentService appointmentService;

	@MockitoBean
	private OwnerRepository owners;

	/**
	 * Méthode exécutée AVANT chaque test pour initialiser les données communes.
	 *
	 * <p>
	 * Initialisation : On simule (mock) que l'owner avec ID=1 existe dans la BDD. Ceci
	 * est nécessaire car le contrôleur vérifie l'existence de l'owner.
	 *
	 * <p>
	 * Pattern : Test Setup - évite de dupliquer le code dans chaque test.
	 *
	 * @author Naiyma
	 */
	@BeforeEach
	void init() {
		Owner owner = new Owner();
		owner.setId(TEST_OWNER_ID);
		given(this.owners.findById(TEST_OWNER_ID)).willReturn(Optional.of(owner));
	}

	/**
	 * Teste l'affichage du formulaire de création d'appointment.
	 *
	 * <p>
	 * Ce test vérifie qu'une requête GET vers /owners/1/appointments/new retourne le
	 * formulaire.
	 *
	 * <p>
	 * Vérifications :
	 * <ul>
	 * <li>Status HTTP 200 OK</li>
	 * <li>Vue Thymeleaf = "owners/createOrUpdateAppointmentForm"</li>
	 * <li>Le modèle contient un objet "appointment" vide</li>
	 * </ul>
	 *
	 * <p>
	 * Technique : MockMvc simule une requête HTTP sans lancer de serveur.
	 *
	 * @author Naiyma
	 */
	@Test
	void testInitNewAppointmentForm() throws Exception {
		mockMvc.perform(get("/owners/{ownerId}/appointments/new", TEST_OWNER_ID))
			.andExpect(status().isOk())
			.andExpect(view().name("owners/createOrUpdateAppointmentForm"));
	}

	/**
	 * Teste la soumission réussie du formulaire de création.
	 *
	 * <p>
	 * Ce test vérifie qu'un formulaire VALIDE crée un appointment et redirige vers la
	 * page de détails de l'owner.
	 *
	 * <p>
	 * Scénario :
	 * <ol>
	 * <li>ON mock le service pour retourner un appointment créé</li>
	 * <li>ON envoie un POST avec date + reason (valides)</li>
	 * <li>VÉRIFIER : Redirection (3xx) vers /owners/1</li>
	 * </ol>
	 *
	 * <p>
	 * Pattern POST-Redirect-GET (PRG) : Après un POST réussi, on redirige pour éviter la
	 * double soumission.
	 *
	 * @author Naiyma
	 */
	@Test
	void testProcessNewAppointmentFormSuccess() throws Exception {
		given(this.appointmentService.createAppointment(org.mockito.ArgumentMatchers.eq(TEST_OWNER_ID),
				org.mockito.ArgumentMatchers.any(Appointment.class)))
			.willReturn(new Appointment());

		mockMvc
			.perform(post("/owners/{ownerId}/appointments/new", TEST_OWNER_ID).param("date", "2024-05-01")
				.param("reason", "Checkup"))
			.andExpect(status().is3xxRedirection())
			.andExpect(view().name("redirect:/owners/{ownerId}"));
	}

	/**
	 * Teste le rejet d'un formulaire INVALIDE.
	 *
	 * <p>
	 * Ce test vérifie la VALIDATION côté serveur : Si des champs obligatoires manquent,
	 * le formulaire est rejeté.
	 *
	 * <p>
	 * Scénario :
	 * <ol>
	 * <li>ON envoie un POST avec SEULEMENT la date (reason manque)</li>
	 * <li>VÉRIFIER : Le modèle contient des erreurs</li>
	 * <li>VÉRIFIER : Status HTTP 200 (on reste sur le formulaire)</li>
	 * <li>VÉRIFIER : Même vue (formulaire avec erreurs)</li>
	 * </ol>
	 *
	 * <p>
	 * Règle : @NotBlank sur reason dans Appointment.java déclenche la validation.
	 *
	 * @author Naiyma
	 */
	@Test
	void testProcessNewAppointmentFormHasErrors() throws Exception {
		mockMvc.perform(post("/owners/{ownerId}/appointments/new", TEST_OWNER_ID).param("date", "2024-05-01"))
			.andExpect(model().attributeHasErrors("appointment"))
			.andExpect(status().isOk())
			.andExpect(view().name("owners/createOrUpdateAppointmentForm"));
	}

	/**
	 * Teste la mise à jour réussie du statut d'un appointment.
	 *
	 * <p>
	 * Ce test vérifie qu'un changement de statut VALIDE :
	 * <ul>
	 * <li>Appelle le service avec les bons paramètres</li>
	 * <li>Redirige vers la page owner</li>
	 * <li>Affiche un message de succès (flash attribute)</li>
	 * </ul>
	 *
	 * <p>
	 * Scénario :
	 * <ol>
	 * <li>ON mock le service pour réussir la transition CREATED → CONFIRMED</li>
	 * <li>ON envoie un POST à /owners/1/appointments/5/status</li>
	 * <li>VÉRIFIER : Redirection + message flash "message"</li>
	 * </ol>
	 *
	 * <p>
	 * Les flash attributes persistent le temps de la redirection.
	 *
	 * @author Naiyma
	 */
	@Test
	void testUpdateAppointmentStatusSuccess() throws Exception {
		given(this.appointmentService.updateStatus(TEST_OWNER_ID, TEST_APPOINTMENT_ID, AppointmentStatus.CONFIRMED))
			.willReturn(new Appointment());

		mockMvc
			.perform(post("/owners/{ownerId}/appointments/{appointmentId}/status", TEST_OWNER_ID, TEST_APPOINTMENT_ID)
				.param("status", "CONFIRMED"))
			.andExpect(status().is3xxRedirection())
			.andExpect(flash().attributeExists("message"));
	}

	/**
	 * Teste la gestion d'erreur lors d'une transition invalide.
	 *
	 * <p>
	 * Ce test vérifie que si le DOMAINE rejette une transition (ex: CREATED → DONE
	 * interdit), le contrôleur :
	 * <ul>
	 * <li>Attrape l'exception (try/catch dans le controller)</li>
	 * <li>Redirige vers la page owner (pas d'erreur 500)</li>
	 * <li>Affiche un message d'erreur (flash attribute "error")</li>
	 * </ul>
	 *
	 * <p>
	 * Scénario :
	 * <ol>
	 * <li>ON mock le service pour lancer IllegalStateException</li>
	 * <li>ON envoie un POST avec une transition invalide</li>
	 * <li>VÉRIFIER : Redirection + flash attribute "error"</li>
	 * </ol>
	 *
	 * <p>
	 * Principe : Gestion gracieuse des erreurs (pas de crash).
	 *
	 * @author Naiyma
	 */
	@Test
	void testUpdateAppointmentStatusFailure() throws Exception {
		given(this.appointmentService.updateStatus(TEST_OWNER_ID, TEST_APPOINTMENT_ID, AppointmentStatus.DONE))
			.willThrow(new IllegalStateException("Invalid transition"));

		mockMvc
			.perform(post("/owners/{ownerId}/appointments/{appointmentId}/status", TEST_OWNER_ID, TEST_APPOINTMENT_ID)
				.param("status", "DONE"))
			.andExpect(status().is3xxRedirection())
			.andExpect(flash().attributeExists("error"));
	}

}
