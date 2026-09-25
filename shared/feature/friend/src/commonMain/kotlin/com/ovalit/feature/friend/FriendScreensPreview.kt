package com.ovalit.feature.friend

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.ovalit.core.designsystem.preview.OvalitThemePreview

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun FriendsDarkPreview() {
    OvalitThemePreview(darkTheme = true) { FriendsScreen(FriendPreviewData.friends, {}, {}, {}, {}) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun FriendsLightPreview() {
    OvalitThemePreview(darkTheme = false) { FriendsScreen(FriendPreviewData.friends, {}, {}, {}, {}) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun FriendsEmptyPreview() {
    OvalitThemePreview { FriendsScreen(FriendPreviewData.noFriends, {}, {}, {}, {}) }
}

// 요청 줄은 이름, 설명, 버튼 두 개가 한 줄이라 가장 빡빡하다
@Preview(widthDp = 320, heightDp = 800, fontScale = 1.5f)
@Composable
private fun FriendsSmallLargeFontPreview() {
    OvalitThemePreview { FriendsScreen(FriendPreviewData.friends, {}, {}, {}, {}) }
}

@Preview(widthDp = 390, heightDp = 1000)
@Composable
private fun FriendProfileDarkPreview() {
    OvalitThemePreview(darkTheme = true) { FriendProfileScreen(FriendPreviewData.profile, {}, {}, {}) }
}

@Preview(widthDp = 390, heightDp = 1000)
@Composable
private fun FriendProfileLightPreview() {
    OvalitThemePreview(darkTheme = false) { FriendProfileScreen(FriendPreviewData.profile, {}, {}, {}) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun FriendProfilePrivatePreview() {
    OvalitThemePreview { FriendProfileScreen(FriendPreviewData.privateProfile, {}, {}, {}) }
}

@Preview(widthDp = 320, heightDp = 1000, fontScale = 1.5f)
@Composable
private fun FriendProfileSmallLargeFontPreview() {
    OvalitThemePreview { FriendProfileScreen(FriendPreviewData.profile, {}, {}, {}) }
}
