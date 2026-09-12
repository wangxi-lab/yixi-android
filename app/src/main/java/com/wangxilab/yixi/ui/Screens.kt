package com.wangxilab.yixi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.wangxilab.yixi.AppUiState
import com.wangxilab.yixi.domain.DailyCount
import com.wangxilab.yixi.domain.InterventionResult
import com.wangxilab.yixi.domain.LaunchableApp
import com.wangxilab.yixi.domain.RecentIntervention
import kotlin.math.max

@Composable
fun HomeScreen(state: AppUiState, openAccessibilitySettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        PageHeader(
            title = "一息",
            subtitle = "少一点惯性，多一点选择。",
            action = {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    IconButton(onClick = openAccessibilitySettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = "无障碍设置")
                    }
                }
            },
        )

        Spacer(Modifier.height(20.dp))
        ServiceStatus(state, openAccessibilitySettings)
        BreathOverview(state)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 76.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InlineMetric("选择放下", "${state.statistics.abandonRate}%", Modifier.weight(1f))
            VerticalDivider(Modifier.height(42.dp))
            InlineMetric("累计触发", "${state.statistics.total}", Modifier.weight(1f))
            VerticalDivider(Modifier.height(42.dp))
            InlineMetric("每次呼吸", "10 秒", Modifier.weight(1f))
        }

        HorizontalDivider()
        CompactStatusRow(
            icon = Icons.Rounded.CheckCircle,
            title = if (state.serviceConnected) "运行状态正常" else "等待系统连接",
            detail = if (state.lastEventAt == 0L) {
                "尚未收到窗口切换事件"
            } else {
                "最近检测：${formatTime(state.lastEventAt)}"
            },
        )
    }
}

@Composable
private fun ServiceStatus(state: AppUiState, openAccessibilitySettings: () -> Unit) {
    val enabled = state.accessibilityEnabled
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = openAccessibilitySettings),
        shape = RoundedCornerShape(15.dp),
        color = if (enabled) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = if (enabled) Icons.Rounded.CheckCircle else Icons.Rounded.Accessibility,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = if (enabled) "守护已开启" else "点击启用守护",
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "${state.apps.count { it.monitored }} 个应用",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BreathOverview(state: AppUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(184.dp)
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(144.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(108.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.statistics.total.toString(),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Text(
                            text = "次深呼吸",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.86f),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "你已经为自己停下 ${formatDuration(state.statistics.totalWaitSeconds)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun AppsScreen(apps: List<LaunchableApp>, onToggle: (String, Boolean) -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(apps, query) {
        apps.filter { query.isBlank() || it.label.contains(query, ignoreCase = true) }
    }
    val monitored = filtered.filter { it.monitored }
    val others = filtered.filterNot { it.monitored }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        PageHeader("守护应用", "选择那些容易让你分心的应用。") {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                IconButton(onClick = { query = "" }) {
                    Icon(Icons.Rounded.FilterList, contentDescription = "清除筛选")
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        TextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("搜索应用") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(13.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
        )
        Spacer(Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            if (monitored.isNotEmpty()) {
                item { SectionLabel("已守护 · ${monitored.size}") }
                items(monitored, key = { "selected-${it.packageName}" }) { app ->
                    AppRow(app, onToggle)
                }
                item { Spacer(Modifier.height(14.dp)) }
            }
            item { SectionLabel(if (query.isBlank()) "其他应用" else "搜索结果") }
            items(others, key = { "other-${it.packageName}" }) { app ->
                AppRow(app, onToggle)
            }
            if (filtered.isEmpty()) {
                item {
                    Text(
                        "没有找到这个应用",
                        modifier = Modifier.padding(vertical = 28.dp).fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 8.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun AppRow(app: LaunchableApp, onToggle: (String, Boolean) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle(app.packageName, !app.monitored) }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            AppIcon(app.icon, app.label)
            Text(
                text = app.label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Switch(
                checked = app.monitored,
                onCheckedChange = { onToggle(app.packageName, it) },
            )
        }
    }
}

@Composable
fun StatsScreen(state: AppUiState, clearStatistics: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp,
            top = 12.dp,
            end = 20.dp,
            bottom = 20.dp,
        ),
    ) {
        item {
            PageHeader("统计", "看见每一次停顿带来的变化。") {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    IconButton(onClick = { showConfirm = true }, enabled = state.statistics.total > 0) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = "清空统计")
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = "${state.statistics.abandonRate}%",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "近 30 天选择放下率",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(18.dp))
            WeeklyChart(state.dailyCounts)
            Spacer(Modifier.height(20.dp))
            SectionLabel("最近记录")
        }
        if (state.recent.isEmpty()) {
            item {
                Text(
                    "完成第一次呼吸后，记录会出现在这里。",
                    modifier = Modifier.padding(vertical = 24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(state.recent, key = { "${it.packageName}-${it.triggeredAt}" }) { record ->
            RecentRow(record)
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
private fun WeeklyChart(counts: List<DailyCount>) {
    val maxCount = max(1, counts.maxOfOrNull { it.count } ?: 1)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("每日触发", modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                Icon(
                    Icons.Rounded.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    "过去 7 天",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
                    .semantics {
                        contentDescription = counts.joinToString("，") { "${it.dayLabel}${it.count}次" }
                    },
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom,
            ) {
                counts.forEach { day ->
                    DayBar(day, maxCount, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DayBar(day: DailyCount, maxCount: Int, modifier: Modifier = Modifier) {
    val fraction = if (day.count == 0) 0.035f else day.count.toFloat() / maxCount
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        Text(
            text = day.count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .width(24.dp)
                .weight(1f),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fraction)
                    .clip(RoundedCornerShape(7.dp))
                    .background(
                        if (day.isToday) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    ),
            )
        }
        Spacer(Modifier.height(7.dp))
        Text(
            text = day.dayLabel,
            style = MaterialTheme.typography.labelSmall,
            color = if (day.isToday) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (day.isToday) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun RecentRow(record: RecentIntervention) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(record.appLabel, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                "${formatTime(record.triggeredAt)} · ${record.waitSeconds} 秒",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = when (record.result) {
                InterventionResult.ABANDONED -> "放下了"
                InterventionResult.PROCEEDED -> "继续打开"
                InterventionResult.TIMEOUT -> "已中断"
                InterventionResult.FAIL_OPEN -> "故障放行"
            },
            style = MaterialTheme.typography.labelLarge,
            color = if (record.result == InterventionResult.ABANDONED) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            fontWeight = FontWeight.Medium,
        )
    }
    HorizontalDivider()
}

@Composable
fun PrivacyScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        PageHeader("隐私", "你的停顿，只属于你。")
        Spacer(Modifier.height(22.dp))
        PrivacyHero()
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IconFact(Icons.Rounded.WifiOff, "无法联网", "未申请网络及网络状态权限", Modifier.weight(1f))
            IconFact(Icons.Rounded.VisibilityOff, "不读屏幕", "不能读取界面文字与控件内容", Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IconFact(Icons.Rounded.Storage, "仅存本地", "关闭云备份与设备数据迁移", Modifier.weight(1f))
            IconFact(Icons.Rounded.DeleteSweep, "30 天清理", "原始记录定期自动删除", Modifier.weight(1f))
        }
        Spacer(Modifier.height(18.dp))
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(13.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    Icons.Rounded.Accessibility,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column {
                    Text("唯一敏感授权：无障碍", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "仅识别当前窗口所属应用并显示呼吸页；不能读取内容、代替点击或执行手势。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun PrivacyHero() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(86.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(26.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp),
            )
        }
        Spacer(Modifier.height(15.dp))
        Text(
            "数据不离开这台设备",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "一息没有联网能力，选择与统计仅保存在\nAndroid 应用沙箱中。",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
