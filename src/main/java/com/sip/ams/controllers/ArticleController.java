package com.sip.ams.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.sip.ams.entities.Article;
import com.sip.ams.entities.Provider;
import com.sip.ams.repositories.ArticleRepository;
import com.sip.ams.repositories.ProviderRepository;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import javax.validation.Valid;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping({ "/articles" })
public class ArticleController {
	private final Path root = Paths.get(System.getProperty("user.dir") + "/src/main/resources/static/uploads");

	private final ArticleRepository articleRepository;
	private final ProviderRepository providerRepository;

	@Autowired
	public ArticleController(ArticleRepository articleRepository, ProviderRepository providerRepository) {
		this.articleRepository = articleRepository;
		this.providerRepository = providerRepository;
	}
	/*
	 * @Autowired private ArticleRepository articleRepository;
	 * 
	 * @Autowired private ProviderRepository providerRepository;
	 */

	@GetMapping("/list")
	public List<Article> getAllArticles() {
		return (List<Article>) articleRepository.findAll();
	}

	@PostMapping("/add")
	Article createArticle(@RequestParam("imageFile") MultipartFile file, @RequestParam("label") String label,
			@RequestParam("price") Float price,
			@RequestParam("providerId") Long providerId, @RequestParam("imageName") String imageName) {
		Provider provider = providerRepository.findById(providerId)
				.orElseThrow(() -> new IllegalArgumentException("Invalid provider Id:" + providerId));
		/// part upload
		String newImageName = getSaltString().concat(file.getOriginalFilename());

		try {
			Files.copy(file.getInputStream(), this.root.resolve(newImageName));
		} catch (Exception e) {
			throw new RuntimeException("Could not store the file. Error: " + e.getMessage());
		}
		Article article = new Article(label, price, newImageName);
		article.setProvider(provider);
		articleRepository.save(article);
		return article;
	}

	@PutMapping("/{articleId}")
	public Article updateArticle(@PathVariable Long articleId, @RequestParam("imageFile") MultipartFile file, @RequestParam("label") String label,
			@RequestParam("price") Float price,
			@RequestParam("providerId") Long providerId, @RequestParam("imageName") String imageName) {
return articleRepository.findById(articleId).map(article -> {
			
			// STEP 1 : delete Old Image from server
			String OldImageName = article.getPicture();
					
				////////
					try {
						File f = new File(this.root + "/" + OldImageName); // file to be delete
						if (f.delete()) // returns Boolean value
						{
							System.out.println(f.getName() + " deleted"); // getting and printing the file name
						} else {
							System.out.println("failed");
						}
					} catch (Exception e) {
						e.printStackTrace();
					}

			 /////// END STEP 1
			Provider provider = providerRepository.findById(providerId)
							.orElseThrow(() -> new IllegalArgumentException("Invalid provider Id:" + providerId));
			article.setProvider(provider);
			/// STEP 2 : Upload new image to server
			String newImageName = getSaltString().concat(file.getOriginalFilename());
			try {
				Files.copy(file.getInputStream(), this.root.resolve(newImageName));
			} catch (Exception e) {
				throw new RuntimeException("Could not store the file. Error: " + e.getMessage());
			}
			/// END STEP 2
			
			
			/// STEP 3 : Update article in database
			article.setLabel(label);
			article.setPrice(price);
			article.setPicture(newImageName);
			return articleRepository.save(article);
		}).orElseThrow(() -> new IllegalArgumentException("ProviderId " + providerId + " not found"));
	}

	@DeleteMapping("/delete/{articleId}")
	public ResponseEntity<?> deleteArticle(@PathVariable(value = "articleId") Long articleId) {
		return articleRepository.findById(articleId).map(article -> {
			articleRepository.delete(article);
			return ResponseEntity.ok().build();
		}).orElseThrow(() -> new IllegalArgumentException("Article not found with id " + articleId));
	}

	@GetMapping("/{articleId}")
	public Article getArticle(@PathVariable Long articleId) {

		Optional<Article> p = articleRepository.findById(articleId);
		//System.out.println(p);
		return p.get();

	}

	protected static String getSaltString() {
		String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
		StringBuilder salt = new StringBuilder();
		Random rnd = new Random();
		while (salt.length() < 18) { // length of the random string.
			int index = (int) (rnd.nextFloat() * SALTCHARS.length());
			salt.append(SALTCHARS.charAt(index));
		}
		String saltStr = salt.toString();
		return saltStr;

	}

}
