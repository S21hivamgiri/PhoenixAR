package com.shivam.phoenix

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class WidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {


        for (widgetId in appWidgetIds) {
            val views =  RemoteViews(context.packageName, R.layout.widget_layout)

            // Small logo -> open homepage activity
            val homeIntent = Intent(context, MainActivity::class.java)
            val homePending = PendingIntent.getActivity(
                context,
                0,
                homeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.logoSmall, homePending)

            // Camera button -> open ARActivity directly
            val arIntent = Intent(context, MainActivity::class.java)
            val arPending = PendingIntent.getActivity(
                context,
                1,
                arIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.cameraButton, arPending)

            // Apply update
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}
