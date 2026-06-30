package com.eventfinder.app.client.organizer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.eventfinder.app.R
import com.eventfinder.app.databinding.FragmentQrScannerBinding
import com.eventfinder.app.domain.model.Ticket
import com.eventfinder.app.domain.model.TicketType
import com.eventfinder.app.utils.UserPreferences
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject

@AndroidEntryPoint
class QRScannerFragment : Fragment(R.layout.fragment_qr_scanner) {

    private var _binding: FragmentQrScannerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: QRScannerViewModel by viewModels()

    @Inject
    lateinit var userPreferences: UserPreferences

    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var cameraExecutor: ExecutorService
    private var isProcessing = false
    private var expectedEventId: String? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(context, "Camera permission is required", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentQrScannerBinding.bind(view)
        expectedEventId = arguments?.getString("EVENT_ID")

        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnDismiss.setOnClickListener {
            viewModel.dismissResult()
        }

        binding.btnCheckIn.setOnClickListener {
            val ticket = viewModel.uiState.value.scannedTicket
            if (ticket != null) {
                val organizerId = userPreferences.getUserId()
                viewModel.checkInAttendee(ticket.ticketId, organizerId)
            }
        }

        observeViewModel()
        checkCameraPermission()
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startCamera()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, BarcodeAnalyzer { barcodes ->
                        processBarcodes(barcodes)
                    })
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    viewLifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to start camera: ${e.message}", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun processBarcodes(barcodes: List<Barcode>) {
        if (isProcessing || !viewModel.uiState.value.scannerActive) {
            return
        }

        for (barcode in barcodes) {
            barcode.rawValue?.let { qrCode ->
                isProcessing = true
                val organizerId = userPreferences.getUserId()
                viewModel.validateQRCode(qrCode, organizerId, expectedEventId)
                return
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.isVisible = state.isValidating

                    // Show/hide result card
                    if (state.scannedTicket != null || state.error != null) {
                        showResultCard(state.scannedTicket, state.error, state.checkInSuccess)
                        isProcessing = true
                    } else {
                        binding.cardResult.isVisible = false
                        if (state.scannerActive) {
                            isProcessing = false
                        }
                    }

                    // Update check-in button
                    binding.btnCheckIn.isEnabled = !state.isCheckingIn
                    if (state.isCheckingIn) {
                        binding.btnCheckIn.text = "Checking In..."
                    } else {
                        binding.btnCheckIn.text = "Check In"
                    }
                }
            }
        }
    }

    private fun showResultCard(ticket: Ticket?, error: String?, checkInSuccess: Boolean) {
        binding.cardResult.isVisible = true

        if (checkInSuccess) {
            // Show success state
            binding.ivResultIcon.setImageResource(R.drawable.ic_check)
            binding.ivResultIcon.setColorFilter(
                ContextCompat.getColor(requireContext(), R.color.md_tertiary)
            )
            binding.tvResultTitle.text = "Checked In Successfully!"
            binding.tvResultTitle.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.md_tertiary)
            )
            binding.tvAttendeeName.text = ticket?.userName ?: ""
            binding.layoutTicketDetails.isVisible = false
            binding.btnCheckIn.isVisible = false
            binding.btnDismiss.text = "Done"
        } else if (error != null) {
            // Show error state
            binding.ivResultIcon.setImageResource(R.drawable.ic_close)
            binding.ivResultIcon.setColorFilter(
                ContextCompat.getColor(requireContext(), R.color.md_error)
            )
            binding.tvResultTitle.text = "Error"
            binding.tvResultTitle.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.md_error)
            )
            binding.tvAttendeeName.text = error
            binding.layoutTicketDetails.isVisible = false
            binding.btnCheckIn.isVisible = false
            binding.btnDismiss.text = "Try Again"
        } else if (ticket != null) {
            // Show valid ticket
            binding.ivResultIcon.setImageResource(R.drawable.ic_check)
            binding.ivResultIcon.setColorFilter(
                ContextCompat.getColor(requireContext(), R.color.md_tertiary)
            )
            binding.tvResultTitle.text = "Valid Ticket"
            binding.tvResultTitle.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.md_tertiary)
            )
            binding.tvAttendeeName.text = ticket.userName

            // Show ticket details
            binding.layoutTicketDetails.isVisible = true
            binding.tvTicketType.text = when (ticket.ticketType) {
                TicketType.PUBLIC_RESERVATION -> "Public Event"
                TicketType.FREE_PRIVATE -> "Free Ticket"
                TicketType.PAID -> "Paid Ticket - ${ticket.currency} ${ticket.purchasePrice}"
            }
            binding.tvTicketId.text = ticket.ticketId.take(8).uppercase()
            binding.btnCheckIn.isVisible = true
            binding.btnDismiss.text = "Cancel"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
        _binding = null
    }

    /**
     * Barcode Analyzer using ML Kit
     */
    private class BarcodeAnalyzer(
        private val onBarcodesDetected: (List<Barcode>) -> Unit
    ) : ImageAnalysis.Analyzer {

        private val scanner = BarcodeScanning.getClient()

        @androidx.camera.core.ExperimentalGetImage
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )

                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        if (barcodes.isNotEmpty()) {
                            onBarcodesDetected(barcodes)
                        }
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }
    }
}
