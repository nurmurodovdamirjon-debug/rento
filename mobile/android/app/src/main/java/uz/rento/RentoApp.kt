package uz.rento

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * RentoApp — Hilt Application class.
 *
 * @HiltAndroidApp annotatsiyasi dependency injection
 * uchun component generatsiya qiladi.
 */
@HiltAndroidApp
class RentoApp : Application()
