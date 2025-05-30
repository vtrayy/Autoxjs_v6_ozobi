package org.autojs.autoxjs.ui.floating.layoutinspector

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.lifecycle.ViewTreeViewModelStoreOwner
import androidx.recyclerview.widget.RecyclerView
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.Theme
import com.google.android.material.snackbar.Snackbar
import com.stardust.app.DialogUtils
import com.stardust.enhancedfloaty.FloatyService
import com.stardust.util.ClipboardUtil
import com.stardust.util.Ozobi
import com.stardust.util.ViewUtils
import com.stardust.view.accessibility.NodeInfo
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.autojs.autoxjs.R
import org.autojs.autoxjs.ui.codegeneration.CodeGenerateDialog
import org.autojs.autoxjs.ui.compose.theme.AutoXJsTheme
import org.autojs.autoxjs.ui.floating.FloatyWindowManger
import org.autojs.autoxjs.ui.floating.FullScreenFloatyWindow
import org.autojs.autoxjs.ui.floating.MyLifecycleOwner
import org.autojs.autoxjs.ui.main.drawer.isNightModeNormal
import org.autojs.autoxjs.ui.widget.BubblePopupMenu
import org.autojs.autoxjs.ui.widget.LevelBeamView
import org.autojs.autoxjs.ui.widget.OnItemClickListener
import pl.openrnd.multilevellistview.ItemInfo
import pl.openrnd.multilevellistview.MultiLevelListView
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Created by Stardust on 2017/3/12.
 */
open class LayoutHierarchyFloatyWindow(private val mRootNode: NodeInfo) : FullScreenFloatyWindow() {
    private var mLayoutHierarchyView: LayoutHierarchyView? = null
    private var mNodeInfoDialog: MaterialDialog? = null
    private var mBubblePopMenu: BubblePopupMenu? = null
    private var mNodeInfoView: NodeInfoView? = null
    private var mContext: Context? = null
    
    private var nightMode = false
    private var firstNodeList = mutableListOf<NodeInfo>()
    private var secondNodeList = mutableListOf<NodeInfo>()
    private var isAuth = false
    private var firstTime = true
    // <
    
    fun getNodeList(nodeInfo:NodeInfo?, nodeList:MutableList<NodeInfo>){
        if(nodeInfo == null){
            return
        }
        nodeList.add(nodeInfo)
        getNodeList(nodeInfo.parent, nodeList)
    }
    fun tagNode():String?{
        if(firstTagNodeInfo == null){
            firstTagNodeInfo = mSelectedNode
            return null
        }
        secondTagNodeInfo = mSelectedNode
        val result = genRelationShip(firstTagNodeInfo, secondTagNodeInfo)
        firstTagNodeInfo = null
        secondTagNodeInfo = null
        return result
    }
    fun genRelationShip(first:NodeInfo?, second:NodeInfo?):String?{
        if(first == null || second == null){
            return null
        }
        if(first == second){
            return null
        }
        try{
            val key = Ozobi.bytesToSecretKey(Ozobi.AESKey,"AES")
            val data1Arr = byteArrayOf(127,126,-46,-71,38,80,83,127,25,10,-124,105,48,85,-78,-33)
            val data2Arr = byteArrayOf(114,-8,-61,58,42,120,-112,36,-51,34,-116,-65,65,-118,-123,-90)
            val str1 = Ozobi.bytesToStr(Ozobi.decrypt(data1Arr,key,"AES"))
            val str2 = Ozobi.bytesToStr(Ozobi.decrypt(data2Arr,key,"AES"))
            getNodeList(first, firstNodeList)
            getNodeList(second, secondNodeList)
            firstNodeList.reverse()
            secondNodeList.reverse()
            var min = firstNodeList.size
            if(secondNodeList.size < firstNodeList.size){
                min = secondNodeList.size
            }
            var crossIndex = min - 1
            for(index in 0 until min){
                if(firstNodeList[index] != secondNodeList[index]){
                    crossIndex = index - 1
                    break
                }
            }
            var parentString = ""
            for(index in crossIndex until firstNodeList.size){
                if(index < 0){
                    return null
                }
                if(firstNodeList[index] == firstTagNodeInfo){
                    break
                }
                parentString += str1
            }
            var childString = ""
            for(index in crossIndex until secondNodeList.size){
                if(secondNodeList[index] == secondTagNodeInfo){
                    break
                }
                if(index+1 < secondNodeList.size){
                    childString += str2 + secondNodeList[index+1].indexInParent + ")"
                }
            }
            val result = parentString + childString
            return result
        }catch(e:Exception){
            return null
        }
    }
    // <
    override fun onCreateView(floatyService: FloatyService): View {
        mContext = ContextThemeWrapper(floatyService, R.style.AppTheme)
        isAuth = Ozobi.authenticate(mContext)
        nightMode = isNightModeNormal(mContext)
        LevelBeamView.nightMode = nightMode
        
        BubblePopupMenu.nightMode = nightMode
        NodeInfoView.nightMode = nightMode
        LayoutHierarchyView.nightMode = nightMode
        // <
        mLayoutHierarchyView = LayoutHierarchyView(mContext)
        val view = ComposeView(mContext!!).apply {
            isFocusableInTouchMode = true
            isFocusable = true
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP ) {
                    showLayoutBounds()
                    return@setOnKeyListener true
                }
                return@setOnKeyListener false
            }
        }
        view.setContent {
            AutoXJsTheme {
                Content()
            }
        }
        // Trick The ComposeView into thinking we are tracking lifecycle
        val viewModelStore = ViewModelStore()
        val lifecycleOwner = MyLifecycleOwner()
        lifecycleOwner.performRestore(null)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        // ozobi - 2025/03/06 修复: 无法触发 compose重组 >
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        // <
        ViewTreeLifecycleOwner.set(view, lifecycleOwner)
        ViewTreeViewModelStoreOwner.set(view) { viewModelStore }
        view.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        view.requestFocus()
        return view
    }
    @Composable
    private fun CurSelectedBounds(rect:Rect, offset: Offset){
        Box(
            modifier = Modifier.offset {
                IntOffset(offset.x.roundToInt(), offset.y.roundToInt())
            }
        ){
            Canvas(modifier = Modifier.fillMaxSize()) {
                try{
                    drawRect(
                        color = Color(0xffff66ff),
                        topLeft = Offset(rect.left.toFloat(), rect.top.toFloat()),
                        size = Size(rect.width().toFloat(), rect.height().toFloat()),
                        style = Stroke(
                            width = mLayoutHierarchyView!!.boundsPaint?.strokeWidth
                                ?: 3f
                        )
                    )
                }catch(e:Exception){
                    Log.e("ozobiLog","LayoutHierarchyFloatyWindow: CurSelectedBounds: $e")
                }
            }
        }
    }
    @Composable
    fun ConnectLine(start:Offset, end:Offset){
        Canvas(modifier = Modifier.fillMaxSize()) {
            try{
                drawLine(color = Color(0xffff66ff),start,end,3.dp.toPx())
            }catch(e:Exception){
                Log.e("ozobiLog","LayoutHierarchyFloatyWindow: ConnectLine: $e")
            }
        }
    }
    @OptIn(DelicateCoroutinesApi::class)
    @Composable
    private fun Content() {
        val context = LocalContext.current
        var isShowLayoutHierarchyView by remember {
            mutableStateOf(true)
        }
        var offset by remember { mutableStateOf(Offset.Zero) }
        var castSpellText by remember {
            mutableStateOf(
                if(firstTagNodeInfo == null){
                    context.getString(R.string.text_tag_node)
                }else{
                    context.getString(R.string.text_spell_casting)
                }
            )
        }
        val (globalPosition, setGlobalPosition) = remember { mutableStateOf(Offset.Zero) }
        var rect by remember { mutableStateOf(Rect()) }
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (mLayoutHierarchyView!!.mShowClickedNodeBounds) {
                    mLayoutHierarchyView!!.mClickedNodeInfo?.let { it ->
                        val statusBarHeight = mLayoutHierarchyView!!.mStatusBarHeight
                        rect = Rect(it.boundsInScreen)
                        rect.offset(0, -statusBarHeight)
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRect(
                                color = Color(
                                    mLayoutHierarchyView!!.boundsPaint?.color ?: 0x2cd0d1
                                ),
                                topLeft = Offset(rect.left.toFloat(), rect.top.toFloat()),
                                size = Size(rect.width().toFloat(), rect.height().toFloat()),
                                style = Stroke(
                                    width = mLayoutHierarchyView!!.boundsPaint?.strokeWidth
                                        ?: 3f
                                )
                            )
                        }
                        CurSelectedBounds(rect, offset)
                        ConnectLine(globalPosition, Offset(rect.centerX().toFloat(),rect.centerY().toFloat()))
                    }
                }
            }
            Column(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = {
                        mLayoutHierarchyView!!
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    update = {
                        it.alpha = if (isShowLayoutHierarchyView) 1f else 0f
                    }
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset {
                            IntOffset(offset.x.roundToInt(), offset.y.roundToInt())
                        }
                        .padding(6.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                            .background(color = Color(0xeeBA3636), shape = RoundedCornerShape(8.dp))
                            .clickable {
                                close()
                            }
                    ) {
                        Text(
                            text = stringResource(R.string.text_exit_floating_window),
                            color=Color.White,
                            modifier = Modifier
                                .padding(7.dp)
                        )
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                            .background(color = Color(0xee7461BF), shape = RoundedCornerShape(8.dp))
                            .clickable {
                                showLayoutBounds()
                            }
                    ) {
                        Text(
                            text = stringResource(R.string.text_show_layout_bounds_window),
                            color=Color.White,
                            modifier = Modifier
                                .padding(7.dp)
                        )
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .background(color = Color(0xee5AA6B5), shape = RoundedCornerShape(8.dp))
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                            .clickable {
                                isShowLayoutHierarchyView = !isShowLayoutHierarchyView
                                GlobalScope.launch {
                                    delay(100L)
                                    mLayoutHierarchyView?.mAdapter?.reloadData()
                                }
                            }
                            .onGloballyPositioned {
                                setGlobalPosition(it.positionInRoot())
                            }
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = {Offset->
                                        isShowLayoutHierarchyView = false
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        offset += dragAmount
                                    },
                                    onDragEnd = {
                                        offset = Offset(0f, 0f)
                                        isShowLayoutHierarchyView = true
                                        GlobalScope.launch {
                                            delay(100L)
                                            mLayoutHierarchyView?.mAdapter?.reloadData()
                                        }
                                    },
                                    onDragCancel = {
                                        offset = Offset(0f, 0f)
                                        isShowLayoutHierarchyView = true
                                    }
                                )
                            }
                    ) {
                        Text(
                            text = stringResource(R.string.text_hide_and_show),
                            color=Color.White,
                            modifier = Modifier
                                .padding(7.dp)
                        )
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                            .background(color = Color(0xee315FA8), shape = RoundedCornerShape(8.dp))
                            .clickable {
                                if (isAuth) {
                                    val result = tagNode()
                                    var hint = "真相已躺好在粘贴板[vscode]"
                                    if (result == null) {
                                        if (firstTagNodeInfo == null) {
                                            val ranKey =
                                                floor(Math.random() * selfHints.size).toInt()
                                            hint = selfHints[ranKey]
                                            castSpellText =
                                                context.getString(R.string.text_tag_node)
                                        } else {
                                            hint = "ozobi: 真相只有一个, 是哪个呢"
                                            castSpellText =
                                                context.getString(R.string.text_spell_casting)
                                        }
                                    } else {
                                        castSpellText =
                                            context.getString(R.string.text_tag_node)
                                        ClipboardUtil.setClip(mContext, result)
                                    }
                                    mLayoutHierarchyView?.let {
                                        val snackBar =
                                            Snackbar.make(it, hint, Snackbar.LENGTH_SHORT)
                                        if (nightMode) {
                                            snackBar.setTextColor(0xff000000.toInt())
                                            snackBar.setBackgroundTint(0xffcfcfcf.toInt())
                                        }
                                        snackBar.show()
                                    }
                                }
                            }
                    ) {
                        Text(
                            text = castSpellText,
                            color = Color.White,
                            modifier = Modifier
                                .padding(7.dp)
                        )
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                            .background(color = Color(0xee53BA5C), shape = RoundedCornerShape(8.dp))
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = {
                                        canCollapse = !canCollapse
                                        var hint = "ozobi: " + context.getString(R.string.text_not_allow_collapse)
                                        if (canCollapse) {
                                            hint = "ozobi: " + context.getString(R.string.text_allow_collapse)
                                        }
                                        mLayoutHierarchyView?.let {
                                            val snackBar =
                                                Snackbar.make(it, hint, Snackbar.LENGTH_SHORT)
                                            if (nightMode) {
                                                snackBar.setTextColor(0xff000000.toInt())
                                                snackBar.setBackgroundTint(0xffcfcfcf.toInt())
                                            }
                                            snackBar.show()
                                        }
                                    },
                                    onDragEnd = {
                                        offset = Offset(0f, 0f)
                                    },
                                    onDragCancel = {
                                        offset = Offset(0f, 0f)
                                    },
                                    onDrag = { change: PointerInputChange, dragAmount: Offset ->
                                        change.consume()
                                        offset += dragAmount
                                    }
                                )
                            }
                            .clickable {
                                expandAll()
                            }
                    ) {
                        Text(
                            text = stringResource(R.string.text_expand_all_hierarchy),
                            color=Color.White,
                            modifier = Modifier
                                .padding(7.dp)
                        )
                    }
                    if(firstTime){
                        firstTime = false
                        mLayoutHierarchyView?.let {
                            val snackBar =  Snackbar.make(it, "ozobi: [展开]、[隐/显] 可以拖动哦", Snackbar.LENGTH_SHORT)
                            if(nightMode){
                                snackBar.setTextColor(0xff000000.toInt())
                                snackBar.setBackgroundTint(0xffcfcfcf.toInt())
                            }
                            snackBar.addCallback(object : Snackbar.Callback(){
                                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                                    super.onDismissed(transientBottomBar, event)
                                    mLayoutHierarchyView!!.mAdapter?.reloadData()
                                }
                            })
                            snackBar.show()
                            GlobalScope.launch {
                                delay(300L)
                                mLayoutHierarchyView!!.mAdapter?.reloadData()
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(v: View) {
        
        if(nightMode){
            mLayoutHierarchyView!!.setBackgroundColor(0xaa666666.toInt())
        }else{
            mLayoutHierarchyView!!.setBackgroundColor(0xaa888888.toInt())
        }
        // <
        mLayoutHierarchyView!!.setShowClickedNodeBounds(true)
        mLayoutHierarchyView!!.boundsPaint?.strokeWidth = 3f
        mLayoutHierarchyView!!.boundsPaint?.color = -0x2cd0d1
        mLayoutHierarchyView!!.setOnItemClickListener(object : OnItemClickListener,
            pl.openrnd.multilevellistview.OnItemClickListener {
                override fun onItemClicked(
                    parent: MultiLevelListView?,
                    view: View?,
                    item: Any?,
                    itemInfo: ItemInfo?
            ) {
                val nodeInfo = item as NodeInfo
                setSelectedNode(nodeInfo)
                if (view != null) {
                    mLayoutHierarchyView!!.setSelectedNodeTouch(nodeInfo)
                }
            }

            override fun onGroupItemClicked(
                parent: MultiLevelListView?,
                view: View?,
                item: Any?,
                itemInfo: ItemInfo?
            ) {
                val nodeInfo = item as NodeInfo
                setSelectedNode(nodeInfo)
                if (view != null) {
                    mLayoutHierarchyView!!.setSelectedNodeTouch(nodeInfo)
                }
            }
            override fun onItemClick(parent: RecyclerView?, item: View?, position: Int) {
                
            }
        })
        // <
        mLayoutHierarchyView!!.setOnItemLongClickListener { view: View, nodeInfo: NodeInfo ->
            mSelectedNode = nodeInfo
            ensureOperationPopMenu()
            if (mBubblePopMenu!!.contentView.measuredWidth <= 0) mBubblePopMenu!!.preMeasure()
            mBubblePopMenu!!.showAsDropDown(
                view,
                view.width / 2 - mBubblePopMenu!!.contentView.measuredWidth / 2,
                -(ViewUtils.spToPx(mContext,45f)).toInt()
            )
        }
        mLayoutHierarchyView!!.setRootNode(mRootNode)
        mSelectedNode?.let { mLayoutHierarchyView!!.setSelectedNodeInit(it) }
    }

    private fun ensureOperationPopMenu() {
        if (mBubblePopMenu != null) return
        mBubblePopMenu = BubblePopupMenu(
            mContext, listOf(
                mContext!!.getString(R.string.text_show_widget_infomation),
                mContext!!.getString(R.string.text_generate_code),
                mContext!!.getString(R.string.text_show_layout_bounds)
            )
        )
        mBubblePopMenu!!.setOnItemClickListener { view: View?, position: Int ->
            mBubblePopMenu!!.dismiss()
            when (position) {
                0 -> {
                    showNodeInfo()
                }
                1 -> {
                    generateCode()
                }
                else -> {
                    showLayoutBounds()
                }
            }
        }
        mBubblePopMenu!!.width = ViewGroup.LayoutParams.WRAP_CONTENT
        mBubblePopMenu!!.height = ViewGroup.LayoutParams.WRAP_CONTENT
    }

    private fun generateCode() {
        DialogUtils.showDialog(
            CodeGenerateDialog(mContext!!, mRootNode, mSelectedNode)
                .build()
        )
    }
    private fun expandAll() {
        mLayoutHierarchyView!!.expand()
    }
    private fun showLayoutBounds() {
        close()
        val window = LayoutBoundsFloatyWindow(mRootNode)
        window.setSelectedNode(mSelectedNode)
        FloatyService.addWindow(window)
    }

    fun showNodeInfo() {
        ensureNodeInfoDialog()
        mNodeInfoView!!.setNodeInfo(mSelectedNode!!)
        mNodeInfoDialog!!.show()
    }

    private fun ensureNodeInfoDialog() {
        var theme = Theme.LIGHT
        if(nightMode){
            theme = Theme.DARK
        }
        if (mNodeInfoDialog == null) {
            mNodeInfoView = NodeInfoView(mContext!!)
            mNodeInfoDialog = MaterialDialog.Builder(mContext!!)
                .customView(mNodeInfoView!!, false)
                .theme(theme)
                .build()
            mNodeInfoDialog!!.window?.setType(FloatyWindowManger.getWindowType())
        }
    }

    fun setSelectedNode(selectedNode: NodeInfo?) {
        mSelectedNode = selectedNode
    }

    companion object {
        private const val TAG = "FloatingHierarchyView"
        
        var canCollapse = true
        var firstTagNodeInfo:NodeInfo? = null
        var secondTagNodeInfo:NodeInfo? = null
        var mSelectedNode: NodeInfo? = null
        var curSelectedNodeChildren : List<NodeInfo>? = null
        var curSelectedNodeParents = mutableListOf<NodeInfo?>()
        var curSelectedBrotherList : List<NodeInfo>? = null
        private var selfHints:Array<String> = arrayOf(
            "饶了地球一圈, 终是回到原点",
            "没有两片完全相同的树叶, 但我是节点",
            "我还是我, 颜色不一样的烟火",
            "谁把我的闪现偷了",
            "没有困难? 那我造一点吧",
            "你掉的是这个金节点, 还是这个银节点",
            "禁止对麻瓜使用魔法",
            "不是枪没压好, 是想打天上的鸟",
            "还说没有开挂, 自瞄哪来的",
            "谎言不会伤人, 所以我想说, 你手抖了"
        )
        // <
    }
}