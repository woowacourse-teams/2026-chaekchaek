package com.chaekchaek.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.chaekchaek.app.domain.profile.AuthorProfile

/** 목적지 entry가 현재 화면에 묶인 이동 콜백을 제공한다. */
internal val LocalOpenAuthorLibrary = staticCompositionLocalOf<(AuthorProfile) -> Unit> { {} }

@Composable
internal fun Modifier.authorProfileLink(profile: AuthorProfile): Modifier {
    val open = LocalOpenAuthorLibrary.current
    return clickable(
        interactionSource = remember { MutableInteractionSource() }, indication = null,
        role = Role.Button, onClick = { open(profile) },
    ).semantics { contentDescription = "${profile.displayName} 프로필" }
}

/** Figma 책췍/Avatar의 기본 이니셜 표현을 재사용한다. 익명에는 사진을 요청하지 않는다. */
@Composable
internal fun AuthorProfileAvatar(profile: AuthorProfile, size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Box(
        modifier.authorProfileLink(profile).then(Modifier.size(size)).clip(androidx.compose.foundation.shape.CircleShape)
            .background(androidx.compose.ui.graphics.Color(0xFFE1E1E5)),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        androidx.compose.material3.Text(
            if (profile.anonymous) "익" else profile.displayName.take(1),
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = if (size.value <= 18) 9.sp else 12.sp,
        )
        profile.visibleImageUrl?.takeIf { it.isNotBlank() }?.let { url ->
            coil3.compose.AsyncImage(model = url, contentDescription = null,
                modifier = Modifier.matchParentSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
        }
    }
}
