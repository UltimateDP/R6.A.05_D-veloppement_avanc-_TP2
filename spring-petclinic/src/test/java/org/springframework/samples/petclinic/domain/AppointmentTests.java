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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Classe de tests unitaires pour l'entité {@link Appointment}.
 *
 * Cette classe teste les règles métier (business rules) du domaine Appointment, notamment
 * la machine à états (state machine) qui gère les transitions de statut.
 *
 * <p>
 * Les tests vérifient que :
 * <ul>
 * <li>Les transitions autorisées fonctionnent correctement</li>
 * <li>Les transitions interdites lèvent des exceptions</li>
 * <li>Les cas limites (null, même statut) sont gérés</li>
 * </ul>
 *
 * <p>
 * Architecture hexagonale : Ces tests sont dans la couche DOMAIN car ils testent la
 * logique métier pure sans dépendances techniques.
 *
 * @author Naiyma
 */
class AppointmentTests {

	/**
	 * Teste la transition CREATED → CONFIRMED.
	 *
	 * <p>
	 * Ce test vérifie qu'un rendez-vous créé (statut par défaut = CREATED) peut être
	 * confirmé par le propriétaire ou la clinique.
	 *
	 * <p>
	 * Étapes du test :
	 * <ol>
	 * <li>Créer un appointment (statut par défaut = CREATED)</li>
	 * <li>Appeler changeStatus(CONFIRMED)</li>
	 * <li>Vérifier que le statut est bien CONFIRMED</li>
	 * </ol>
	 *
	 * <p>
	 * Résultat attendu : La transition est autorisée et le statut change.
	 *
	 * @author Naiyma
	 */
	@Test
	void shouldAllowCreatedToConfirmed() {
		// GIVEN : Un rendez-vous créé avec le statut par défaut CREATED
		Appointment appointment = new Appointment();

		// WHEN : On confirme le rendez-vous
		appointment.changeStatus(AppointmentStatus.CONFIRMED);

		// THEN : Le statut doit être CONFIRMED
		assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
	}

	/**
	 * Teste la transition CREATED → CANCELLED.
	 *
	 * <p>
	 * Ce test vérifie qu'un rendez-vous créé peut être annulé (par exemple si le
	 * propriétaire change d'avis).
	 *
	 * <p>
	 * Étapes du test :
	 * <ol>
	 * <li>Créer un appointment (statut = CREATED)</li>
	 * <li>Appeler changeStatus(CANCELLED)</li>
	 * <li>Vérifier que le statut est bien CANCELLED</li>
	 * </ol>
	 *
	 * <p>
	 * Résultat attendu : La transition est autorisée. CANCELLED est un état terminal (on
	 * ne peut plus en sortir).
	 *
	 * @author Naiyma
	 */
	@Test
	void shouldAllowCreatedToCancelled() {
		// GIVEN : Un rendez-vous créé
		Appointment appointment = new Appointment();

		// WHEN : On annule le rendez-vous
		appointment.changeStatus(AppointmentStatus.CANCELLED);

		// THEN : Le statut doit être CANCELLED
		assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
	}

	/**
	 * Teste la transition CONFIRMED → DONE.
	 *
	 * <p>
	 * Ce test vérifie qu'un rendez-vous confirmé peut être marqué comme terminé après que
	 * la consultation ait eu lieu.
	 *
	 * <p>
	 * Étapes du test :
	 * <ol>
	 * <li>Créer un appointment et le confirmer (CREATED → CONFIRMED)</li>
	 * <li>Appeler changeStatus(DONE)</li>
	 * <li>Vérifier que le statut est bien DONE</li>
	 * </ol>
	 *
	 * <p>
	 * Résultat attendu : La transition est autorisée. DONE est un état terminal (le
	 * rendez-vous est terminé).
	 *
	 * @author Naiyma
	 */
	@Test
	void shouldAllowConfirmedToDone() {
		// GIVEN : Un rendez-vous confirmé
		Appointment appointment = new Appointment();
		appointment.changeStatus(AppointmentStatus.CONFIRMED);

		// WHEN : On marque le rendez-vous comme terminé
		appointment.changeStatus(AppointmentStatus.DONE);

		// THEN : Le statut doit être DONE
		assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.DONE);
	}

	/**
	 * Teste le rejet de la transition CREATED → DONE.
	 *
	 * <p>
	 * Ce test vérifie qu'on ne peut PAS passer directement de CREATED à DONE. Il faut
	 * d'abord confirmer le rendez-vous (CREATED → CONFIRMED → DONE).
	 *
	 * <p>
	 * Règle métier : Un rendez-vous doit être confirmé avant d'être terminé.
	 *
	 * <p>
	 * Étapes du test :
	 * <ol>
	 * <li>Créer un appointment (statut = CREATED)</li>
	 * <li>Essayer de passer directement à DONE</li>
	 * <li>Vérifier qu'une IllegalStateException est levée</li>
	 * </ol>
	 *
	 * <p>
	 * Résultat attendu : Une exception IllegalStateException est levée.
	 *
	 * @author Naiyma
	 */
	@Test
	void shouldRejectCreatedToDone() {
		// GIVEN : Un rendez-vous créé
		Appointment appointment = new Appointment();

		// WHEN : On essaie de le marquer comme terminé sans le confirmer
		// THEN : Une exception doit être levée
		assertThatThrownBy(() -> appointment.changeStatus(AppointmentStatus.DONE))
			.isInstanceOf(IllegalStateException.class);
	}

	/**
	 * Teste le rejet de la transition DONE → CONFIRMED.
	 *
	 * <p>
	 * Ce test vérifie qu'on ne peut PAS revenir en arrière depuis DONE. Une fois le
	 * rendez-vous terminé, on ne peut plus changer son statut.
	 *
	 * <p>
	 * Règle métier : DONE et CANCELLED sont des états terminaux.
	 *
	 * <p>
	 * Étapes du test :
	 * <ol>
	 * <li>Créer un appointment et le faire passer à DONE (CREATED → CONFIRMED →
	 * DONE)</li>
	 * <li>Essayer de revenir à CONFIRMED</li>
	 * <li>Vérifier qu'une IllegalStateException est levée</li>
	 * </ol>
	 *
	 * <p>
	 * Résultat attendu : Une exception est levée car DONE est terminal.
	 *
	 * @author Naiyma
	 */
	@Test
	void shouldRejectDoneToConfirmed() {
		// GIVEN : Un rendez-vous terminé
		Appointment appointment = new Appointment();
		appointment.changeStatus(AppointmentStatus.CONFIRMED);
		appointment.changeStatus(AppointmentStatus.DONE);

		// WHEN : On essaie de le re-confirmer
		// THEN : Une exception doit être levée (état terminal)
		assertThatThrownBy(() -> appointment.changeStatus(AppointmentStatus.CONFIRMED))
			.isInstanceOf(IllegalStateException.class);
	}

	/**
	 * Teste que changer vers le même statut est autorisé (no-op).
	 *
	 * <p>
	 * Ce test vérifie qu'on peut appeler changeStatus avec le statut actuel sans erreur.
	 * C'est une opération neutre (no-op = no operation).
	 *
	 * <p>
	 * Cas d'usage : Éviter les erreurs si on reçoit plusieurs fois la même commande.
	 *
	 * <p>
	 * Étapes du test :
	 * <ol>
	 * <li>Créer un appointment (statut = CREATED par défaut)</li>
	 * <li>Appeler changeStatus(CREATED)</li>
	 * <li>Vérifier que le statut reste CREATED sans erreur</li>
	 * </ol>
	 *
	 * <p>
	 * Résultat attendu : Pas d'erreur, le statut reste inchangé.
	 *
	 * @author Naiyma
	 */
	@Test
	void shouldIgnoreSameStatus() {
		// GIVEN : Un rendez-vous créé
		Appointment appointment = new Appointment();

		// WHEN : On essaie de le mettre au même statut
		appointment.changeStatus(AppointmentStatus.CREATED);

		// THEN : Le statut reste CREATED sans erreur
		assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CREATED);
	}

	/**
	 * Teste le rejet d'un statut null.
	 *
	 * <p>
	 * Ce test vérifie la validation des paramètres : on ne peut pas passer null à
	 * changeStatus().
	 *
	 * <p>
	 * Principe : Fail-fast - détecter les erreurs le plus tôt possible.
	 *
	 * <p>
	 * Étapes du test :
	 * <ol>
	 * <li>Créer un appointment</li>
	 * <li>Essayer d'appeler changeStatus(null)</li>
	 * <li>Vérifier qu'une IllegalArgumentException est levée</li>
	 * </ol>
	 *
	 * <p>
	 * Résultat attendu : IllegalArgumentException (pas NullPointerException). On valide
	 * explicitement les paramètres.
	 *
	 * @author Naiyma
	 */
	@Test
	void shouldRejectNullStatus() {
		// GIVEN : Un rendez-vous créé
		Appointment appointment = new Appointment();

		// WHEN : On essaie de passer un statut null
		// THEN : Une IllegalArgumentException doit être levée
		assertThatThrownBy(() -> appointment.changeStatus(null)).isInstanceOf(IllegalArgumentException.class);
	}

}
