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

import java.util.Optional;

import org.springframework.samples.petclinic.adapter.out.OwnerRepository;
import org.springframework.samples.petclinic.application.AppointmentService;
import org.springframework.samples.petclinic.domain.Appointment;
import org.springframework.samples.petclinic.domain.AppointmentStatus;
import org.springframework.samples.petclinic.domain.Owner;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

/**
 * Controleur MVC pour gerer les rendez-vous depuis l'interface web. Il prepare les objets
 * pour les formulaires et appelle le service applicatif.
 *
 * @author Naiyma
 */
@Controller
class AppointmentController {

	private final AppointmentService appointmentService;

	private final OwnerRepository owners;

	public AppointmentController(AppointmentService appointmentService, OwnerRepository owners) {
		this.appointmentService = appointmentService;
		this.owners = owners;
	}

	/**
	 * Interdit le binding du champ id pour eviter qu'il soit modifie depuis le
	 * formulaire.
	 * @param dataBinder binder Spring MVC
	 */
	@InitBinder
	public void setAllowedFields(WebDataBinder dataBinder) {
		dataBinder.setDisallowedFields("id");
	}

	/**
	 * Charge l'owner a partir de l'URL. Sert a afficher les infos proprietaire dans la
	 * vue.
	 * @param ownerId id du proprietaire
	 * @return owner charge depuis la base
	 */
	@ModelAttribute("owner")
	public Owner loadOwner(@PathVariable("ownerId") int ownerId) {
		Optional<Owner> optionalOwner = owners.findById(ownerId);
		return optionalOwner.orElseThrow(() -> new IllegalArgumentException(
				"Owner not found with id: " + ownerId + ". Please ensure the ID is correct "));
	}

	/**
	 * Cree un objet Appointment vide pour le formulaire.
	 * @return rendez-vous par defaut
	 */
	@ModelAttribute("appointment")
	public Appointment initAppointment() {
		return new Appointment();
	}

	/**
	 * Affiche le formulaire de creation d'un rendez-vous.
	 * @return template Thymeleaf
	 */
	@GetMapping("/owners/{ownerId}/appointments/new")
	public String initNewAppointmentForm() {
		return "owners/createOrUpdateAppointmentForm";
	}

	/**
	 * Traite la soumission du formulaire et cree le rendez-vous. En cas d'erreurs de
	 * validation, on revient au formulaire.
	 * @param ownerId id du proprietaire
	 * @param appointment donnees du formulaire
	 * @param result erreurs de validation
	 * @param redirectAttributes messages flash
	 * @return redirection vers la fiche owner
	 */
	@PostMapping("/owners/{ownerId}/appointments/new")
	public String processNewAppointmentForm(@PathVariable("ownerId") int ownerId, @Valid Appointment appointment,
			BindingResult result, RedirectAttributes redirectAttributes) {
		if (result.hasErrors()) {
			return "owners/createOrUpdateAppointmentForm";
		}

		this.appointmentService.createAppointment(ownerId, appointment);
		redirectAttributes.addFlashAttribute("message", "Appointment created");
		return "redirect:/owners/{ownerId}";
	}

	/**
	 * Change le statut d'un rendez-vous depuis la page owner. Si la transition est
	 * interdite, un message d'erreur est affiche.
	 * @param ownerId id du proprietaire
	 * @param appointmentId id du rendez-vous
	 * @param status nouveau statut
	 * @param redirectAttributes messages flash
	 * @return redirection vers la fiche owner
	 */
	@PostMapping("/owners/{ownerId}/appointments/{appointmentId}/status")
	public String updateAppointmentStatus(@PathVariable("ownerId") int ownerId,
			@PathVariable("appointmentId") int appointmentId, @RequestParam("status") AppointmentStatus status,
			RedirectAttributes redirectAttributes) {
		try {
			this.appointmentService.updateStatus(ownerId, appointmentId, status);
			redirectAttributes.addFlashAttribute("message", "Appointment status updated");
		}
		catch (IllegalArgumentException | IllegalStateException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/owners/{ownerId}";
	}

}
