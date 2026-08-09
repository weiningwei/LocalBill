package com.localbill

import android.app.Application
import com.localbill.db.LocalBillDB
import com.localbill.util.Prefs

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        Prefs.init(this)
        db = LocalBillDB(this)
    }

    companion object {
        lateinit var instance: App
            private set
        lateinit var db: LocalBillDB
            private set
    }
}
