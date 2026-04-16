package com.example.aibookreader.presentation.screens.reader

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ActionMode
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commitNow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.aibookreader.R
import com.example.aibookreader.databinding.FragmentReaderBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.readium.r2.navigator.OverflowableNavigator
import org.readium.r2.navigator.SelectableNavigator
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.util.DirectionalNavigationAdapter
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Url

/**
 * Обёртка над [EpubNavigatorFragment]: Readium рендерит EPUB в WebView и отдаёт [Locator] при смене позиции.
 * Связываем навигатор с ViewModel (прогресс, [EpubPreferences], жест «вперёд/назад» по спайну).
 */
@OptIn(org.readium.r2.shared.ExperimentalReadiumApi::class)
@AndroidEntryPoint
class ReadiumReaderFragment : Fragment(), EpubNavigatorFragment.Listener {

    private var _binding: FragmentReaderBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReadiumReaderViewModel by activityViewModels()

    /** Встроенный фрагмент Readium: навигация по ресурсам и выделение текста (SelectableNavigator). */
    private var navigator: EpubNavigatorFragment? = null

    /** WebView из иерархии навигатора — для тапа по контенту без перехвата у Readium. */
    private var chromeTapWebView: WebView? = null

    private val aiMenuId = View.generateViewId()
    private val copyMenuId = android.R.id.copy
    private val selectAllMenuId = android.R.id.selectAll

    private val selectionCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            menu.add(0, copyMenuId, 0, android.R.string.copy)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            menu.add(0, selectAllMenuId, 1, android.R.string.selectAll)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            menu.add(0, aiMenuId, 2, "✨ ИИ")
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = true

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            val nav = navigator as? SelectableNavigator ?: return false

            when (item.itemId) {
                aiMenuId -> {
                    viewLifecycleOwner.lifecycleScope.launch {
                        // SelectableNavigator: выделение уже упаковано в Locator (в т.ч. text.highlight)
                        val selection = nav.currentSelection() ?: return@launch
                        val text = selection.locator.text.highlight ?: return@launch
                        if (text.isNotBlank()) {
                            viewModel.setSelectedText(text)
                        }
                        nav.clearSelection()
                    }
                    mode.finish()
                    return true
                }
                copyMenuId -> {
                    viewLifecycleOwner.lifecycleScope.launch {
                        val selection = nav.currentSelection() ?: return@launch
                        val text = selection.locator.text.highlight ?: return@launch
                        if (text.isNotBlank()) {
                            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("book_text", text))
                            Toast.makeText(requireContext(), "Скопировано", Toast.LENGTH_SHORT).show()
                        }
                        nav.clearSelection()
                    }
                    mode.finish()
                    return true
                }
                selectAllMenuId -> {
                    return false
                }
            }
            return false
        }

        override fun onDestroyActionMode(mode: ActionMode) {}
    }

    override fun onExternalLinkActivated(url: AbsoluteUrl) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url.toString()))
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val factory = viewModel.uiState.value.navigatorFactory

        // Пока Publication не готов — заглушка, иначе FragmentManager требует factory до super.onCreate()
        if (factory == null) {
            childFragmentManager.fragmentFactory = EpubNavigatorFragment.createDummyFactory()
            super.onCreate(savedInstanceState)
            return
        }

        // Readium: фабрика создаёт EpubNavigatorFragment с publication и начальным Locator
        childFragmentManager.fragmentFactory = factory.createFragmentFactory(
            initialLocator = viewModel.uiState.value.initialLocator,
            listener = this,
            configuration = EpubNavigatorFragment.Configuration(
                selectionActionModeCallback = selectionCallback,
                shouldApplyInsetsPadding = false
            )
        )

        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReaderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (viewModel.uiState.value.navigatorFactory == null) return

        val tag = NAVIGATOR_TAG

        if (savedInstanceState == null) {
            childFragmentManager.commitNow {
                add(R.id.reader_container, EpubNavigatorFragment::class.java, Bundle(), tag)
            }
        }

        navigator = childFragmentManager.findFragmentByTag(tag) as? EpubNavigatorFragment

        navigator?.let { nav ->
            // Свайпы/тапы по краю экрана → goForward/goBackward (overflow pagination)
            nav.addInputListener(DirectionalNavigationAdapter(nav))

            var lastTapAttachHref: Url? = null
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    // Flow позиции от Readium — источник правды для прогресса и контекста ИИ
                    nav.currentLocator
                        .onEach { loc ->
                            viewModel.onLocationChanged(loc)
                            if (loc.href != lastTapAttachHref) {
                                lastTapAttachHref = loc.href
                                scheduleAttachTapToToggleChrome()
                            }
                        }
                        .launchIn(this)

                    viewModel.uiState
                        .onEach { state -> nav.submitPreferences(state.epubPreferences) }
                        .launchIn(this)

                    // Кнопки полосы: программная смена «страницы» в модели Readium
                    viewModel.navCommands
                        .onEach { cmd ->
                            when (cmd) {
                                NavCommand.Forward -> (nav as? OverflowableNavigator)?.goForward()
                                NavCommand.Backward -> (nav as? OverflowableNavigator)?.goBackward()
                            }
                        }
                        .launchIn(this)
                }
            }
        }

        scheduleAttachTapToToggleChrome()
    }

    private fun scheduleAttachTapToToggleChrome() {
        val nav = navigator ?: return
        val root = nav.view ?: return
        val runnable = Runnable { attachTapToToggleReaderChrome() }
        root.post(runnable)
        root.postDelayed(runnable, 400)
        root.postDelayed(runnable, 1200)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachTapToToggleReaderChrome() {
        val nav = navigator ?: return
        val root = nav.view ?: return
        val wv = findWebViewInHierarchy(root) ?: return
        if (chromeTapWebView === wv) return
        chromeTapWebView?.setOnTouchListener(null)
        chromeTapWebView = wv
        val detector = GestureDetector(
            requireContext(),
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    viewModel.toggleControls()
                    return false
                }
            }
        )
        wv.setOnTouchListener { _, event ->
            detector.onTouchEvent(event)
            false
        }
    }

    private fun findWebViewInHierarchy(view: View?): WebView? {
        if (view == null) return null
        if (view is WebView) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                findWebViewInHierarchy(view.getChildAt(i))?.let { return it }
            }
        }
        return null
    }

    override fun onStop() {
        super.onStop()
        viewModel.saveProgress()
    }

    override fun onDestroyView() {
        chromeTapWebView?.setOnTouchListener(null)
        chromeTapWebView = null
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val NAVIGATOR_TAG = "epub_navigator"
    }
}
