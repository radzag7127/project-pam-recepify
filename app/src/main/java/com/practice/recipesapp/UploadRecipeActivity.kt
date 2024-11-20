package com.practice.recipesapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.practice.recipesapp.databinding.ActivityUploadRecipeBinding
import android.app.ProgressDialog
import com.google.firebase.firestore.ktx.firestore
import com.practice.recipesapp.model.CloudRecipe
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import com.bumptech.glide.Glide

class UploadRecipeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUploadRecipeBinding
    private lateinit var storage: FirebaseStorage
    private lateinit var auth: FirebaseAuth
    private var selectedImageUri: Uri? = null
    private var isEditMode = false
    private var documentId: String? = null
    private var currentImageUrl: String? = null
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            binding.recipeImage.setImageURI(it)
        }
    }
    private val categories = arrayOf("Breakfast", "Lunch", "Dinner", "Dessert")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadRecipeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        storage = Firebase.storage
        auth = FirebaseAuth.getInstance()

        // Check if we're in edit mode
        isEditMode = intent.getBooleanExtra("EDIT_MODE", false)
        if (isEditMode) {
            loadExistingRecipe()
            binding.uploadButton.text = "Update Recipe"
        }

        setupCategorySpinner()
        setupClickListeners()
    }

    private fun setupCategorySpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        binding.categorySpinner.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.backBtn.setOnClickListener {
            finish()
        }

        binding.recipeImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        binding.uploadButton.setOnClickListener {
            if (validateInputs()) {
                uploadRecipe()
            }
        }

        binding.deleteButton.setOnClickListener {
            deleteRecipe()
        }

        binding.deleteButton.visibility = if (isEditMode) View.VISIBLE else View.GONE
    }

    private fun validateInputs(): Boolean {
        if (!isEditMode && selectedImageUri == null && currentImageUrl == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show()
            return false
        }
        if (binding.titleInput.text.isNullOrEmpty()) {
            Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show()
            return false
        }
        if (binding.durationInput.text.isNullOrEmpty()) {
            Toast.makeText(this, "Please enter duration", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!binding.durationInput.text.toString().matches(Regex("\\d+\\s*(mins|minutes|hour|hours)"))) {
            Toast.makeText(this, "Duration format should be like '30 mins' or '2 hours'", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun saveImageToInternalStorage(uri: Uri): String {
        val directory = File(applicationContext.filesDir, "recipe_images")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        
        val fileName = "recipe_${System.currentTimeMillis()}.jpg"
        val file = File(directory, fileName)
        
        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        
        return file.absolutePath
    }

    private fun uploadRecipe() {
        val progressDialog = ProgressDialog(this).apply {
            setMessage(if (isEditMode) "Updating recipe..." else "Uploading recipe...")
            setCancelable(false)
            show()
        }

        if (isEditMode && selectedImageUri == null) {
            createRecipeObject(currentImageUrl!!, progressDialog)
        } else if (selectedImageUri != null) {
            uploadImage(progressDialog)
        }
    }

    private fun createRecipeObject(imageUrl: String, progressDialog: ProgressDialog) {
        val recipe = CloudRecipe(
            img = imageUrl,
            title = binding.titleInput.text.toString(),
            duration = binding.durationInput.text.toString(),
            description = binding.descriptionInput.text.toString(),
            ingredients = binding.ingredientsInput.text.toString()
                .split("\n")
                .filter { it.isNotEmpty() },
            category = binding.categorySpinner.selectedItem.toString(),
            userId = auth.currentUser?.uid ?: ""
        )
        saveRecipeToFirestore(recipe, progressDialog)
    }

    private fun saveRecipeToFirestore(recipe: CloudRecipe, progressDialog: ProgressDialog) {
        val db = Firebase.firestore
        
        if (isEditMode && documentId != null) {
            // Update existing recipe
            db.collection("recipes")
                .document(documentId!!)
                .set(recipe)
                .addOnSuccessListener {
                    progressDialog.dismiss()
                    Toast.makeText(this, "Recipe updated successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    progressDialog.dismiss()
                    Toast.makeText(this, "Failed to update recipe: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Create new recipe
            db.collection("recipes")
                .add(recipe)
                .addOnSuccessListener { documentRef ->
                    // Set the document ID to match its reference
                    documentRef.update("id", documentRef.id)
                        .addOnSuccessListener {
                            progressDialog.dismiss()
                            Toast.makeText(this, "Recipe uploaded successfully", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                }
                .addOnFailureListener { e ->
                    progressDialog.dismiss()
                    Toast.makeText(this, "Failed to save recipe: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadExistingRecipe() {
        documentId = intent.getStringExtra("DOCUMENT_ID")
        currentImageUrl = intent.getStringExtra("RECIPE_IMAGE")
        
        binding.titleInput.setText(intent.getStringExtra("RECIPE_TITLE"))
        binding.descriptionInput.setText(intent.getStringExtra("RECIPE_DESCRIPTION"))
        
        // Parse ingredients and duration from the combined string
        val ingredientsText = intent.getStringExtra("RECIPE_INGREDIENTS") ?: ""
        val lines = ingredientsText.split("\n")
        if (lines.isNotEmpty()) {
            binding.durationInput.setText(lines[0])
            binding.ingredientsInput.setText(lines.drop(1).joinToString("\n"))
        }

        // Load existing image
        Glide.with(this)
            .load(currentImageUrl)
            .into(binding.recipeImage)

        // Set category spinner using the class-level categories array
        val category = intent.getStringExtra("RECIPE_CATEGORY")
        val position = categories.indexOf(category)
        if (position != -1) {
            binding.categorySpinner.setSelection(position)
        }
    }

    private fun deleteOldImage(imageUrl: String) {
        try {
            // Get reference to the old image
            val oldImageRef = storage.getReferenceFromUrl(imageUrl)
            
            oldImageRef.delete()
                .addOnSuccessListener {
                    Log.d("Storage", "Old image deleted successfully")
                }
                .addOnFailureListener { e ->
                    Log.e("Storage", "Error deleting old image", e)
                }
        } catch (e: Exception) {
            Log.e("Storage", "Error getting reference to old image", e)
        }
    }

    private fun uploadImage(progressDialog: ProgressDialog) {
        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val storageRef = storage.reference
            val fileName = "recipe_${System.currentTimeMillis()}.jpg"
            val imageRef = storageRef.child("recipe_images/$fileName")
            val uploadTask = imageRef.putFile(selectedImageUri!!)

            uploadTask
                .continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let { throw it }
                    }
                    imageRef.downloadUrl
                }
                .addOnSuccessListener { downloadUrl ->
                    // If in edit mode and we have an old image URL, delete it
                    if (isEditMode && currentImageUrl != null) {
                        deleteOldImage(currentImageUrl!!)
                    }

                    val recipe = CloudRecipe(
                        img = downloadUrl.toString(),
                        title = binding.titleInput.text.toString(),
                        duration = binding.durationInput.text.toString(),
                        description = binding.descriptionInput.text.toString(),
                        ingredients = binding.ingredientsInput.text.toString()
                            .split("\n")
                            .filter { it.isNotEmpty() },
                        category = binding.categorySpinner.selectedItem.toString(),
                        userId = auth.currentUser?.uid ?: ""
                    )
                    
                    saveRecipeToFirestore(recipe, progressDialog)
                }
                .addOnFailureListener { e ->
                    progressDialog.dismiss()
                    Toast.makeText(this, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("UploadRecipe", "Error uploading image", e)
                }
        } catch (e: Exception) {
            progressDialog.dismiss()
            Toast.makeText(this, "Error processing image: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("UploadRecipe", "Error processing image", e)
        }
    }

    private fun deleteRecipe() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Recipe")
            .setMessage("Are you sure you want to delete this recipe? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                val progressDialog = ProgressDialog(this).apply {
                    setMessage("Deleting recipe...")
                    setCancelable(false)
                    show()
                }

                // Delete from Firestore first
                documentId?.let { docId ->
                    Firebase.firestore.collection("recipes")
                        .document(docId)
                        .delete()
                        .addOnSuccessListener {
                            // If successful, delete the image from storage
                            currentImageUrl?.let { imageUrl ->
                                deleteOldImage(imageUrl)
                            }
                            progressDialog.dismiss()
                            Toast.makeText(this, "Recipe deleted successfully", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            progressDialog.dismiss()
                            Toast.makeText(this, "Failed to delete recipe: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
} 
