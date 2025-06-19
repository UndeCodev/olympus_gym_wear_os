package com.example.olympusgym.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.olympusgym.domain.model.Membership
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MembershipViewModel : ViewModel() {
    // Estado privado
    private val _currentMembership = MutableStateFlow<Membership?>(null)

    // Estado público observable
    val currentMembership: StateFlow<Membership?> = _currentMembership

    // Mensaje de notificación
    private val _notificationMessage = MutableStateFlow<String?>(null)
    val notificationMessage: StateFlow<String?> = _notificationMessage

    init {
        loadMockData() // Carga datos de prueba al iniciar
    }

    private fun loadMockData() {
        viewModelScope.launch {
            // Datos de ejemplo (reemplaza con tu lógica real)
            _currentMembership.value = Membership(
                id = "1",
                userName = "Juan Pérez",
                status = "active",
                expirationDate = "15/08/2023",
                daysRemaining = 7 // Cambia este valor para probar notificaciones
            )
            checkForNotifications()
        }
    }

    private fun checkForNotifications() {
        _currentMembership.value?.let { membership ->
            when (membership.daysRemaining) {
                7 -> _notificationMessage.value = "Tu membresía vence en 1 semana"
                3 -> _notificationMessage.value = "Tu membresía vence en 3 días"
                1 -> _notificationMessage.value = "Tu membresía vence mañana"
            }
        }
    }

    fun clearNotification() {
        _notificationMessage.value = null
    }
}