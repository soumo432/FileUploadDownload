package com.company.controller;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.company.model.Product;
import com.company.other.UploadFileResponse;
import com.company.service.FileStorageService;
import com.company.service.ProductService;

@RestController
@RequestMapping("/product")
public class ProductController {

	@Autowired
	ProductService productService;
	
	@Autowired
    private FileStorageService fileStorageService;

	@GetMapping(value = "getproducts")
	public ResponseEntity<List<Product>> handle2() {

		CacheControl cacheControl = CacheControl.noCache();

		return ResponseEntity.ok().cacheControl(cacheControl).body(productService.getProducts());
	}

	@GetMapping("")
	List<Product> getProducts() {
		return productService.getProducts();
	}

	@GetMapping("/{id}")
	public Product getProduct(@PathVariable("id") Long id) {
		return productService.getProduct(id);
	}

	@PostMapping(value = "")
	public Map<String, Object> createProduct(@RequestParam(value = "id") Long id,
			@RequestParam(value = "name") String name, @RequestParam(value = "price") Integer price) {

		productService.createProduct(id, name, price);

		Map<String, Object> map = new HashMap<String, Object>();
		map.put("status", "Product added!");
		return map;
	}

	@PutMapping(value = "")
	public Product updateProductUsingJson(@RequestBody Product product) {
		productService.updateProduct(product);
		return product;
	}

	@DeleteMapping("/{id}")
	public Map<String, Object> deleteProduct(@PathVariable("id") Long id) {
		productService.deleteProduct(id);
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("status", "Product deleted!");
		return map;
	}
	
	@PatchMapping(value = "/{id}")
	public @ResponseBody void saveManager(@PathVariable Long id, @RequestBody Map<Object, Object> fields) {
		Product product = productService.getProduct(id);
	    // Map key is field name, v is value
	    fields.forEach((k, v) -> {
	       // use reflection to get field k on manager and set it to value k
	        Field field = ReflectionUtils.findField(Product.class, (String) k);
	        field.setAccessible(true);
	        ReflectionUtils.setField(field, product, v);
	    });
	    productService.updateProduct(product);
	}
	
	
    @PostMapping("/uploadFile")
    public UploadFileResponse uploadFile(@RequestParam("file") MultipartFile file) {
        String fileName = fileStorageService.storeFile(file);

        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/product/downloadFile/")
                .path(fileName)
                .toUriString();

        return new UploadFileResponse(fileName, fileDownloadUri,
                file.getContentType(), file.getSize());
    }
    
    @PostMapping("/uploadFiles")
    public List<UploadFileResponse> uploadMultipleFiles(@RequestParam("files") MultipartFile[] files) {
        return Arrays.asList(files)
                .stream()
                .map(file -> uploadFile(file))
                .collect(Collectors.toList());
    }
    
    @GetMapping("/downloadFile/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, HttpServletRequest request) {
        // Load file as Resource
        Resource resource = fileStorageService.loadFileAsResource(fileName);

        // Try to determine file's content type
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            System.out.println(("Could not determine file type."));	
        }

        // Fallback to the default content type if type could not be determined
        if(contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
