package net.toot_counter.web.service

import net.toot_counter.db.entity.TootVisibility

class MessageService {

    fun getLoginMessage(isNewUser: Boolean): String {
        return if (isNewUser) {
            "新規登録しました"
        } else {
            "ログインしました"
        }
    }

    fun getTootVisibilityMessage(visibility: TootVisibility): String {
        when (visibility) {
            TootVisibility.Unlisted -> "未収載"
            TootVisibility.Private -> "非公開"
            TootVisibility.Direct -> "ダイレクトメッセージ"
            else -> throw Error("公開範囲設定ミス")
        }.let {
            return it
        }
    }
}