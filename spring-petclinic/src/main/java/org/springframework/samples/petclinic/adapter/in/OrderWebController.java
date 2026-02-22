package org.springframework.samples.petclinic.adapter.in;

import java.time.LocalDateTime;

import org.springframework.samples.petclinic.adapter.out.OrderRepository;
import org.springframework.samples.petclinic.domain.Order;
import org.springframework.samples.petclinic.domain.OrderStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/orders")
public class OrderWebController {

	private final OrderRepository orderRepository;

	public OrderWebController(OrderRepository orderRepository) {
		this.orderRepository = orderRepository;
	}

	@GetMapping
	public String list(Model model) {
		model.addAttribute("orders", orderRepository.findAll());
		return "orders/ordersList";
	}

	@GetMapping("/new")
	public String newForm() {
		return "orders/orderForm";
	}

	@PostMapping
	public String create(@RequestParam Long petId, @RequestParam String dateTime) {
		Order order = Order.create(petId, LocalDateTime.parse(dateTime));
		orderRepository.save(order);
		return "redirect:/orders";
	}

	@PostMapping("/{id}/status")
	public String changeStatus(@PathVariable Integer id, @RequestParam String status, Model model) {
		Order order = orderRepository.findById(id).orElse(null);
		if (order == null) {
			return "redirect:/orders";
		}

		try {
			order.changeStatus(OrderStatus.valueOf(status));
			orderRepository.save(order);
		}
		catch (Exception e) {
			// ultra simple : on renvoie la liste avec un message d'erreur
			model.addAttribute("orders", orderRepository.findAll());
			model.addAttribute("error", e.getMessage());
			return "orders/ordersList";
		}

		return "redirect:/orders";
	}

}
