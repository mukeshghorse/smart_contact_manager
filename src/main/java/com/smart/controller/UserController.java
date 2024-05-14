package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;
import com.smart.helper.Message;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/user")
public class UserController {

	@Autowired
	BCryptPasswordEncoder bCryptPasswordEncoder;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ContactRepository contactRepository;

	// method for adding common data to response
	@ModelAttribute
	public void addCommonData(Model model, Principal principal) {
		String username = principal.getName();
		System.out.println("username " + username);
		User user = userRepository.getUserByUserName(username);
		System.out.println("USER " + user);
		model.addAttribute("user", user);
	}

	@GetMapping("/index")
	public String dashboard(Model model, Principal principal) {
		model.addAttribute("title", "User dashboard ");
		model.addAttribute("homeSideActive", "active");
		return "normal/user_dashboard";
	}

	@GetMapping("/add-contact")
	public String openAddContactForm(Model model) {
		model.addAttribute("title", "Add Contact");
		model.addAttribute("contact", new Contact());
		model.addAttribute("addContactActive", "active");
		return "normal/add_contact_form";
	}

	@PostMapping("/process-contact")
	public String processContact(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file,
			Principal principal, HttpSession session) {
		try {
			System.out.println("data" + contact);
			String name = principal.getName();
			User user = this.userRepository.getUserByUserName(name);
			// processing and uploading file
			if (file.isEmpty()) {
				System.out.println("file is empty");
				contact.setImage("contact.png");
			} else {
				contact.setImage(file.getOriginalFilename());
				File saveFile = new ClassPathResource("static/img").getFile();

				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				System.out.println("image uploaded");
			}

			contact.setUser(user);
			user.getContacts().add(contact);
			userRepository.save(user);
			System.out.println("added to database");
			session.setAttribute("message", new Message("Contact added successfully!!", "alert-success"));

			return "normal/add_contact_form";
		} catch (Exception e) {
			System.out.println("Error" + e.getMessage());
			e.printStackTrace();
			// error message

		}
		return "normal/add_contact_form";

	}

	// per page=5[n]
	// current page= page

	@GetMapping("/show-contacts/{page}")
	public String showContact(@PathVariable("page") Integer page, Model m, Principal principal) {
		m.addAttribute("title", "View Contacts");
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		Pageable pageable = PageRequest.of(page, 5);
		Page<Contact> contacts = this.contactRepository.findContactByUser(user.getId(), pageable);
		m.addAttribute("contacts", contacts);
		m.addAttribute("currentPage", page);
		m.addAttribute("totalPages", contacts.getTotalPages());
		m.addAttribute("viewContactActive", "active");
		return "normal/show-contacts";
	}

	// Show particular contact detail
	@GetMapping("/{cId}/contact")
	public String showContactDetail(@PathVariable("cId") Integer cId, Model model, Principal principal) {
		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		if (user.getId() == contact.getUser().getId()) {
			model.addAttribute("contact", contact);
			model.addAttribute("title", contact.getName());

		}

		return "normal/contact_detail";
	}

	// Delete contact using contact id
	@GetMapping("/delete/{cId}")
	public String deleteContact(@PathVariable("cId") Integer cId, Principal principal, HttpSession session) {
		Contact contact = this.contactRepository.findById(cId).get();
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);

		if (user.getId() == contact.getUser().getId()) {
			contact.setUser(null);
			// delete contact
			this.contactRepository.delete(contact);
			// remove contact image

			session.setAttribute("msg", new Message("contact deleted successfully....", "alert-success"));

		}
		return "redirect:/user/show-contacts/0";
	}

	// profile page handler
	@GetMapping("/profile")
	public String yourPage(Model model) {
		model.addAttribute("title", "Profile page");
		model.addAttribute("profileActive", "active");
		return "normal/profile";
	}

	@GetMapping("/settings")
	public String settings(Model model) {
		model.addAttribute("settingsActive", "active");
		model.addAttribute("title", "Settings");
		return "normal/setting";
	}

	// password change handler
	@PostMapping("/change-password")
	public String passwordChange(@RequestParam("oldPassword") String oldPassword,
			@RequestParam("newPassword") String newPassword, Principal principal, HttpSession session) {
		String userName = principal.getName();
		User currentUser = this.userRepository.getUserByUserName(userName);
		
			
		if(bCryptPasswordEncoder.matches(oldPassword, currentUser.getPassword())) {
			//password change
			currentUser.setPassword(this.bCryptPasswordEncoder.encode(newPassword));
			this.userRepository.save(currentUser);
			session.setAttribute("message",new Message("Password change successfully", "alert-success"));
		}else {
			
			session.setAttribute("message", new Message("Old password does'nt match", "alert-danger"));
			return "redirect:/user/settings";
		}
		
		
		return "redirect:/user/settings";
	}

}
