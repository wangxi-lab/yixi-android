package com.wangxilab.yixi

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wangxilab.yixi.domain.InterventionResult
import com.wangxilab.yixi.domain.LaunchableApp
import com.wangxilab.yixi.domain.RecentIntervention
import com.wangxilab.yixi.ui.theme.YixiTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            YixiTheme {
                YixiRoot(
                    viewModel = viewModel,
                    openAccessibilitySettings = {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }
}

private enum class Screen(val label: String, val mark: String) {
    HOME("首页", "息"),
    APPS("应用", "选"),
    STATS("统计", "数"),
    PRIVACY("隐私", "隐"),
}

@Composable
private fun YixiRoot(
    viewModel: MainViewModel,
    openAccessibilitySettings: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedIndex by remember { mutableIntStateOf(0) }
    val screens = Screen.entries

    Scaffold(
        bottomBar = {
            NavigationBar {
                screens.forEachIndexed { index, screen ->
                    NavigationBarItem(
                        selected = selectedIndex == index,
                        onClick = { selectedIndex = index },
                        icon = { Text(screen.mark, fontWeight = FontWeight.SemiBold) },
                        label = { Text(screen.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (state.loading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                when (screens[selectedIndex]) {
                    Screen.HOME -> HomeScreen(state, openAccessibilitySettings)
                    Screen.APPS -> AppsScreen(state.apps, viewModel::setMonitored)
                    Screen.STATS -> StatsScreen(state, viewModel::clearStatistics)
                    Screen.PRIVACY -> PrivacyScreen()
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(state: AppUiState, openAccessibilitySettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("一息", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text("打开干扰应用前，先呼吸十秒。", style = MaterialTheme.typography.titleMedium)

        StatusCard(
            enabled = state.accessibilityEnabled,
            connected = state.serviceConnected,
            openAccessibilitySettings = openAccessibilitySettings,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("已守护", "${state.apps.count { it.monitored }} 个", Modifier.weight(1f))
            MetricCard("成功放下", "${state.statistics.abandonRate}%", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("触发次数", "${state.statistics.total}", Modifier.weight(1f))
            MetricCard("呼吸时间", "${state.statistics.totalWaitSeconds} 秒", Modifier.weight(1f))
        }

        SectionCard("realme UI 7.0 检查") {
            Text("1. 在无障碍设置中启用“一息 · 应用打开提醒”。")
            Text("2. 在电池/应用耗电管理中允许一息后台活动。")
            Text("3. 锁屏再解锁后打开一个已守护应用，确认呼吸页出现。")
            Spacer(Modifier.height(4.dp))
            Text(
                if (state.lastEventAt == 0L) "尚未收到窗口切换事件" else "最近事件：${formatTime(state.lastEventAt)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatusCard(
    enabled: Boolean,
    connected: Boolean,
    openAccessibilitySettings: () -> Unit,
) {
    val container = if (enabled) Color(0xFFE0EEE7) else Color(0xFFFFE8D9)
    Card(colors = CardDefaults.cardColors(containerColor = container)) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                if (enabled) "守护已开启" else "还差一步：启用无障碍服务",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                when {
                    connected -> "服务正在运行，只接收窗口所属应用的包名。"
                    enabled -> "系统已授权；服务可能正在被系统重新连接。"
                    else -> "无需联网、悬浮窗、使用情况或存储权限。"
                },
            )
            Button(onClick = openAccessibilitySettings) {
                Text(if (enabled) "检查无障碍设置" else "去启用")
            }
        }
    }
}

@Composable
private fun AppsScreen(apps: List<LaunchableApp>, onToggle: (String, Boolean) -> Unit) {
    var query by remember { mutableStateOf("") }
    val visibleApps = remember(apps, query) {
        apps.filter {
            query.isBlank() || it.label.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
        }.sortedByDescending { it.monitored }
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(20.dp)) {
            Text("选择需要守护的应用", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("列表来自系统启动器入口，不读取使用记录。")
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("搜索应用或包名") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        HorizontalDivider()
        LazyColumn(Modifier.fillMaxSize()) {
            items(visibleApps, key = { it.packageName }) { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(app.label, style = MaterialTheme.typography.titleMedium)
                        Text(
                            app.packageName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = app.monitored,
                        onCheckedChange = { onToggle(app.packageName, it) },
                    )
                }
                HorizontalDivider(Modifier.padding(start = 20.dp))
            }
        }
    }
}

@Composable
private fun StatsScreen(state: AppUiState, clearStatistics: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("本地统计", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("仅保留最近 30 天，不会上传。")
            }
            TextButton(onClick = { showConfirm = true }, enabled = state.statistics.total > 0) {
                Text("清空")
            }
        }
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MetricCard("选择放下", "${state.statistics.abandoned}", Modifier.weight(1f))
            MetricCard("选择继续", "${state.statistics.proceeded}", Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        LazyColumn(Modifier.fillMaxSize()) {
            if (state.recent.isEmpty()) {
                item { Text("完成第一次呼吸后，记录会出现在这里。", Modifier.padding(20.dp)) }
            }
            items(state.recent) { record -> RecentRow(record) }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("清空本地统计？") },
            text = { Text("全部呼吸记录会从这台设备上永久删除。") },
            confirmButton = {
                TextButton(onClick = {
                    clearStatistics()
                    showConfirm = false
                }) { Text("清空") }
            },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun RecentRow(record: RecentIntervention) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(record.appLabel, style = MaterialTheme.typography.titleMedium)
            Text(
                "${formatTime(record.triggeredAt)} · ${record.waitSeconds} 秒",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            when (record.result) {
                InterventionResult.ABANDONED -> "放下了"
                InterventionResult.PROCEEDED -> "继续打开"
                InterventionResult.TIMEOUT -> "已中断"
                InterventionResult.FAIL_OPEN -> "故障放行"
            },
            color = if (record.result == InterventionResult.ABANDONED) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
    HorizontalDivider(Modifier.padding(start = 20.dp))
}

@Composable
private fun PrivacyScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("隐私设计", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("一息的功能边界写进了应用清单和无障碍配置，不只是一份承诺。")
        PrivacyItem("无联网权限", "应用无法直接访问互联网，也不读取网络状态。")
        PrivacyItem("不读取窗口内容", "canRetrieveWindowContent=false，只处理窗口切换事件携带的包名与时间。")
        PrivacyItem("不申请悬浮窗", "呼吸页使用系统为无障碍服务提供的专用覆盖层。")
        PrivacyItem("不读使用情况", "不申请 PACKAGE_USAGE_STATS，也不保存非目标应用包名。")
        PrivacyItem("本机保存", "选择列表与统计位于 Android 应用沙箱；云备份和设备迁移均已禁用。")
        PrivacyItem("默认 30 天", "原始统计自动清理；你也可以随时在“统计”页全部删除。")
        SectionCard("唯一敏感授权") {
            Text("无障碍权限用于获知当前窗口属于哪个应用，并显示呼吸页。服务不能读取界面文字、不能代替你点击，也不能执行手势。")
        }
    }
}

@Composable
private fun PrivacyItem(title: String, description: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

private fun formatTime(timestamp: Long): String = DateTimeFormatter
    .ofPattern("MM-dd HH:mm")
    .withZone(ZoneId.systemDefault())
    .format(Instant.ofEpochMilli(timestamp))
