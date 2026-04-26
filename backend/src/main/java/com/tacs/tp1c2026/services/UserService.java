package com.tacs.tp1c2026.services;


import com.tacs.tp1c2026.repositories.UserRepository;


import org.springframework.stereotype.Service;

@Service
public class UserService {

	  private final UserRepository userRepository;

	  public UserService(UserRepository userRepository){
		  this.userRepository = userRepository;
	  }



}

