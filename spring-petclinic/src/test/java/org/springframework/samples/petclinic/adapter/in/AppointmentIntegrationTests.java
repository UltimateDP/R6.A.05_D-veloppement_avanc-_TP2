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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.samples.petclinic.PetClinicApplication;
import org.springframework.samples.petclinic.adapter.out.AppointmentRepository;
import org.springframework.samples.petclinic.adapter.out.OwnerRepository;
import org.springframework.samples.petclinic.domain.Appointment;
import org.springframework.samples.petclinic.domain.AppointmentStatus;
import org.springframework.samples.petclinic.domain.Owner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Classe de tests d'INTÉGRATION pour le flux complet des appointments.
 *
 * Cette classe teste le système END-TO-END (bout en bout) avec toutes les couches réelles
 * : Contrôleur → Service → Repository → BDD.
 *
 * <p>
 * Type de tests : Tests d'INTÉGRATION avec @SpringBootTest
 * <ul>
 * <li>Spring charge le contexte COMPLET (tous les beans)</li>
 * <li>Base de données H2 en mémoire (réelle, pas mockée)</li>
 * <li>Toute la logique métier s'exécute vraiment</li>
 * <li>Tests plus lents mais plus réalistes</li>
 * </ul>
 *
 * <p>
 * Différence avec les tests unitaires :
 * <table>
 * <tr>
 * <th>Tests Unitaires</th>
 * <th>Tests Intégration</th>
 * </tr>
 * <tr>
 * <td>Mocks</td>
 * <td>Beans réels</td>
 * </tr>
 * <tr>
 * <td>Rapides</td>
 * <td>Plus lents</td>
 * </tr>
 * <tr>
 * <td>Isolés</td>
 * <td>End-to-end</td>
 * </tr>
 * <tr>
 * <td>Test 1 classe</td>
 * <td>Test le système</td>
 * </tr>
 * </table>
 *
 * <p>
 * Ce qu'on teste ici :
 * <ul>
 * <li>Le flux HTTP complet (requête → réponse)</li>
 * <li>La persistance VRAIE en BDD</li>
 * <li>Les transactions</li>
 * <li>L'intégration entre toutes les couches</li>
 * </ul>
 *
 * <p>
 * Architecture hexagonale : Ces tests TRAVERSENT toutes les couches (Adapter.In →
 * Application → Domain → Adapter.Out).
 *
 * @author Naiyma
 */
@SpringBootTest(classes = PetClinicApplication.class)
class AppointmentIntegrationTests {

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Autowired
	private OwnerRepository owners;

	@Autowired
	private AppointmentRepository appointments;

	/**
	 * Configuration exécutée AVANT chaque test.
	 *
	 * <p>
	 * On initialise MockMvc à partir du contexte Spring complet. Cela permet de simuler
	 * des requêtes HTTP sans démarrer un serveur web.
	 *
	 * <p>
	 * Différence avec @WebMvcTest : Ici on utilise webAppContextSetup (contexte complet),
	 * pas standaloneSetup (controller seul).
	 *
	 * @author Naiyma
	 */
	@BeforeEach
	void setup() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
	}

	/**
	 * Teste la création COMPLÈTE d'un appointment via le formulaire web.
	 *
	 * <p>
	 * Ce test vérifie le FLUX END-TO-END :
	 * <ol>
	 * <li>Requête HTTP POST vers le contrôleur</li>
	 * <li>Le contrôleur appelle le service</li>
	 * <li>Le service vérifie l'owner et initialise le statut</li>
	 * <li>Le repository sauvegarde en BDD H2 (VRAIE BDD)</li>
	 * <li>Redirection vers /owners/1</li>
	 * <li>Message flash de succès</li>
	 * <li>Vérification en BDD : l'appointment existe bien</li>
	 * </ol>
	 *
	 * <p>
	 * Vérifications :
	 * <ul>
	 * <li>HTTP 3xx (redirection)</li>
	 * <li>Vue = "redirect:/owners/{ownerId}"</li>
	 * <li>Flash attribute "message" existe</li>
	 * <li>findByOwnerId(1) retourne au moins 1 appointment</li>
	 * </ul>
	 *
	 * <p>
	 * Pourquoi ce test est important : Il détecte les problèmes d'intégration que les
	 * tests unitaires ne voient pas.
	 *
	 * @author Naiyma
	 */
	@Test
	void shouldCreateAppointmentFromForm() throws Exception {
		mockMvc
			.perform(post("/owners/{ownerId}/appointments/new", 1).param("date", "2024-05-01")
				.param("reason", "Checkup"))
			.andExpect(status().is3xxRedirection())
			.andExpect(view().name("redirect:/owners/{ownerId}"))
			.andExpect(flash().attributeExists("message"));

		assertThat(this.appointments.findByOwnerId(1)).isNotEmpty();
	}

	/**
	 * Teste la mise à jour COMPLÈTE du statut via le formulaire web.
	 *
	 * <p>
	 * Ce test vérifie le FLUX DE MISE À JOUR end-to-end :
	 * <ol>
	 * <li>Créer un appointment en BDD (via repository)</li>
	 * <li>Envoyer un POST pour changer le statut</li>
	 * <li>Le contrôleur appelle le service</li>
	 * <li>Le service vérifie ownership et délègue au domaine</li>
	 * <li>Le domaine valide la transition CREATED → CONFIRMED</li>
	 * <li>Le repository met à jour en BDD</li>
	 * <li>Redirection avec message de succès</li>
	 * <li>Vérification en BDD : le statut a VRAIMENT changé</li>
	 * </ol>
	 *
	 * <p>
	 * Vérifications :
	 * <ul>
	 * <li>HTTP 3xx (redirection)</li>
	 * <li>Flash attribute "message"</li>
	 * <li>En BDD : appointment.getStatus() == CONFIRMED</li>
	 * </ul>
	 *
	 * <p>
	 * Particularité : On interroge la BDD AVANT et APRÈS pour vérifier que la transaction
	 * a bien persisté les changements.
	 *
	 * @author Naiyma
	 */
	@Test
	void shouldUpdateAppointmentStatusFromForm() throws Exception {
		Owner owner = this.owners.findById(1).orElseThrow();
		Appointment appointment = new Appointment();
		appointment.setOwner(owner);
		appointment.setDate(LocalDate.now());
		appointment.setReason("Annual check");
		appointment.setStatus(AppointmentStatus.CREATED);
		appointment = this.appointments.save(appointment);

		mockMvc
			.perform(post("/owners/{ownerId}/appointments/{appointmentId}/status", 1, appointment.getId())
				.param("status", "CONFIRMED"))
			.andExpect(status().is3xxRedirection())
			.andExpect(view().name("redirect:/owners/{ownerId}"))
			.andExpect(flash().attributeExists("message"));

		Appointment updated = this.appointments.findById(appointment.getId()).orElseThrow();
		assertThat(updated.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
	}

	/**
	 * Teste le REJET d'une transition invalide dans un flux end-to-end.
	 *
	 * <p>
	 * Ce test vérifie que les RÈGLES MÉTIER du DOMAINE sont bien respectées dans un flux
	 * complet avec BDD réelle.
	 *
	 * <p>
	 * Scénario :
	 * <ol>
	 * <li>Créer un appointment CREATED en BDD</li>
	 * <li>Essayer de passer directement à DONE (interdit !)</li>
	 * <li>Le domaine lève IllegalStateException</li>
	 * <li>Le service propage l'exception</li>
	 * <li>Le contrôleur attrape l'exception</li>
	 * <li>Redirection avec message d'ERREUR</li>
	 * </ol>
	 *
	 * <p>
	 * Vérifications :
	 * <ul>
	 * <li>HTTP 3xx (redirection, pas d'erreur 500)</li>
	 * <li>Flash attribute "error" existe</li>
	 * <li>L'utilisateur voit un message clair</li>
	 * </ul>
	 *
	 * <p>
	 * Importance : Ce test prouve que les règles métier du domaine sont bien appliquées
	 * dans le système réel, pas seulement dans les tests unitaires.
	 *
	 * @author Naiyma
	 */
	@Test
	void shouldRejectInvalidStatusChangeFromForm() throws Exception {
		Owner owner = this.owners.findById(1).orElseThrow();
		Appointment appointment = new Appointment();
		appointment.setOwner(owner);
		appointment.setDate(LocalDate.now());
		appointment.setReason("Vaccination");
		appointment.setStatus(AppointmentStatus.CREATED);
		appointment = this.appointments.save(appointment);

		mockMvc
			.perform(post("/owners/{ownerId}/appointments/{appointmentId}/status", 1, appointment.getId())
				.param("status", "DONE"))
			.andExpect(status().is3xxRedirection())
			.andExpect(flash().attributeExists("error"));
	}

}
