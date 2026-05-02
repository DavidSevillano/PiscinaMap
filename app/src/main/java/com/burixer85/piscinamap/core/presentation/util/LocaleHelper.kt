package com.burixer85.piscinamap.core.presentation.util

import android.content.Context
import com.burixer85.piscinamap.R
import java.util.Locale

object LocaleHelper {

    fun getLocale(context: Context): Locale {
        return context.resources.configuration.locales[0]
    }

    fun isSpanish(context: Context): Boolean {
        return getLocale(context).language == "es"
    }

    fun getString(context: Context, resId: Int): String {
        return context.getString(resId)
    }

    fun getString(context: Context, resId: Int, vararg formatArgs: Any): String {
        return context.getString(resId, *formatArgs)
    }
}