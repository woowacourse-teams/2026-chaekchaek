package com.chamsae.chaekchaek

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import java.util.UUID

/** 실제 외부 실행마다 ID를 붙이고 기존 앱 task로 전달한다. URI 해석은 공통 계층에서 한다. */
class BookDeepLinkActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.action == Intent.ACTION_VIEW && intent.data != null) {
            startActivity(Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = intent.data
                putExtra(RECEIPT, UUID.randomUUID().toString())
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
        }
        finish()
    }

    companion object { internal const val RECEIPT = "book_deep_link_receipt" }
}
