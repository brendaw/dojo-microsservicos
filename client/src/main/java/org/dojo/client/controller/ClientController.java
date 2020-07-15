package org.dojo.client.controller;

import org.dojo.client.entity.Client;
import org.dojo.client.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("clients")
public class ClientController {

    @Autowired
    private ClientRepository clientRepository;

    @GetMapping
    public List<Client> viewAllClients() {
        return clientRepository.findAll();
    }

    @PostMapping
    public Client createClient(@RequestBody Client client) {
        return clientRepository.saveAndFlush(client);
    }

    @GetMapping("/{id}")
    public Client viewClient(@PathVariable Long id) {
        return clientRepository.findById(id).get();
    }

    @GetMapping("/legal-document/{legalDocument}")
    public Client getClientByLegalDocument(@PathVariable String legalDocument) {
        return Optional.ofNullable(clientRepository.findByLegalDocument(legalDocument))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/{id}")
    public Client updateClient(@PathVariable Long id, @RequestBody Client client) {
        client.setId(id);

        return clientRepository.saveAndFlush(client);
    }

    @DeleteMapping("/{id}")
    public void deleteClient(@PathVariable Long id) {
        clientRepository.deleteById(id);
    }

}
