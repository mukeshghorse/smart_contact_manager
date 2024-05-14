package com.smart.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smart.dao.UserRepository;
import com.smart.entities.User;
import com.smart.helper.Message;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;






@Controller
public class HomeController {
	@Autowired
	private BCryptPasswordEncoder passwordEncoder;
	
	@Autowired
	private UserRepository userRepository;
	
	@GetMapping("/")
	public String home(Model model) {
		model.addAttribute("title", "home - Smart Contact Manager");
		model.addAttribute("homeActive", "active");
		return "home";
	}
	
	

	@GetMapping("/about")
	public String about(Model model) {
		model.addAttribute("title", "About - Smart Contact Manager");
		model.addAttribute("aboutActive", "active");

		return "about";
	}

	@GetMapping("/signup")
	public String signup(Model model) {
		model.addAttribute("title", "Signup - Smart Contact Manager");
		model.addAttribute("signupActive", "active");
		model.addAttribute("user", new User());
		return "signup";
	}

	@GetMapping("/login")
	public String login(Model model) {
		model.addAttribute("title" , "Login - Smart Contact Manager"); 
		model.addAttribute("loginActive","active");
		return "login";
	}

	// handler for registering user
	@PostMapping("/doRegister")
	public String registerUser(@Valid @ModelAttribute("user") User user,BindingResult result, @RequestParam(value = "agreement",defaultValue ="false") boolean agreement, Model model, HttpSession session)
	{
		try {
			
			if(!agreement) {
				System.out.println("you have not agreed the term and condition");
				throw new Exception("you have not agreed the term and condition");
			}
			if(result.hasErrors()) {
				model.addAttribute("user",user);

				System.out.println(result);
				return "signup";
			}
			
			user.setRole("ROLE_USER");
			user.setEnabled(true);
			user.setImgUrl("default.png");
			user.setPassword(passwordEncoder.encode(user.getPassword()));
			
			userRepository.save(user);
			model.addAttribute("user",new User());
			session.setAttribute("message", new Message("Successfully Registered","alert-success"));

			
			return "signup";
		} catch (Exception e) {
			e.printStackTrace();
			model.addAttribute("user",user);
			session.setAttribute("message", new Message("something went wrong !!" +e.getMessage(),"alert-danger"));
			return "signup";
		}
		
		
	}
	
}
