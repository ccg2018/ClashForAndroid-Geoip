package stub

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        BarUtils.setNavBarVisibility(this, window, false)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        layout.setOnClickListener {
            finish()
        }

        msgTv.text = "${getString(R.string.package_label)} \nversion ${getString(R.string.geoip_version)}"
        Handler(Looper.getMainLooper()).postDelayed({
            finish()
        }, 4000L)
    }

    override fun overridePendingTransition(enterAnim: Int, exitAnim: Int) {
        super.overridePendingTransition(0, 0)
    }

}