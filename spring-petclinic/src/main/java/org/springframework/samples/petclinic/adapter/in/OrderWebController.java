package org.springframework.samples.petclinic.adapter.in;

import java.time.LocalDateTime;

import org.springframework.samples.petclinic.adapter.out.OrderRepository;
import org.springframework.samples.petclinic.domain.Order;
import org.springframework.samples.petclinic.domain.OrderStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

// @author Mathias Verhaeghem-lipson
// Contrôleur web pour la gestion des commandes
@Controller
@RequestMapping("/orders")
public class OrderWebController {

	// Repository d'accès aux données pour les commandes
	private final OrderRepository orderRepository;

	// Injection du repository via le constructeur
	public OrderWebController(OrderRepository orderRepository) {
		this.orderRepository = orderRepository;
	}

	// Endpoint pour afficher la liste de toutes les commandes
	@GetMapping
	public String list(Model model) {
		// Récupération de toutes les commandes et ajout à l'attribut "orders" du modèle
		model.addAttribute("orders", orderRepository.findAll());
		return "orders/ordersList";
	}

	// Endpoint pour afficher le formulaire de création d'une nouvelle commande
	@GetMapping("/new")
	public String newForm() {
		return "orders/orderForm";
	}

	// Endpoint pour créer une nouvelle commande à partir des données du formulaire
	@PostMapping
	public String create(@RequestParam Long petId, @RequestParam String dateTime) {
		// Création d'une nouvelle commande à partir des paramètres reçus
		Order order = Order.create(petId, LocalDateTime.parse(dateTime));
		orderRepository.save(order);
		return "redirect:/orders";
	}

	// Endpoint pour changer le statut d'une commande spécifiée par son id
	@PostMapping("/{id}/status")
	public String changeStatus(@PathVariable Integer id, @RequestParam String status, Model model) {

		// Recherche de la commande par son id, si elle n'existe pas on redirige vers la
		// liste des commandes
		Order order = orderRepository.findById(id).orElse(null);
		if (order == null) {
			return "redirect:/orders";
		}

		// Tentative de changement de statut, si une exception est levée
		// (ex: statut invalide ou transition interdite) on redirige vers la liste des
		// commandes
		// avec un message d'erreur
		try {
			order.changeStatus(OrderStatus.valueOf(status));
			orderRepository.save(order);
		}
		catch (Exception e) {
			// En cas d'erreur, on recharge la liste des commandes et
			// on ajoute le message d'erreur au modèle
			model.addAttribute("orders", orderRepository.findAll());
			model.addAttribute("error", e.getMessage());
			return "orders/ordersList";
		}

		return "redirect:/orders";
	}

}
