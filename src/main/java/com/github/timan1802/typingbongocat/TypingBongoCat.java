package com.github.timan1802.typingbongocat;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.TypedAction;
import com.intellij.openapi.editor.actionSystem.TypedActionHandler;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(
    name = "TypingBongoCat",
    storages = @Storage("typing-bongo-cat.xml")
)
@Service(Service.Level.APP)
public final class TypingBongoCat implements PersistentStateComponent<TypingBongoCat>, Disposable {
    @com.intellij.util.xmlb.annotations.Transient
    private final TypedActionHandler originalHandler;

    private boolean enabled = true;

    public TypingBongoCat() {
        final TypedAction typedAction = TypedAction.getInstance();
        TypedActionHandler currentHandler = typedAction.getRawHandler();

        // --- 수정된 부분: 핸들러 중첩 방지 로직 ---
        // 플러그인 동적 리로드 시, 이미 우리 핸들러가 설정되어 있는지 확인합니다.
        if (currentHandler instanceof TypingBongoCatTypedActionHandler) {
            // 만약 그렇다면, 그 안에 저장된 '진짜' 원본 핸들러를 가져옵니다.
            this.originalHandler = ((TypingBongoCatTypedActionHandler) currentHandler).getOriginalHandler();
        } else {
            // 그렇지 않다면, 현재 핸들러가 원본 핸들러입니다.
            this.originalHandler = currentHandler;
        }

        // 이제 중첩 걱정 없이 안전하게 새 핸들러를 설정합니다.
        typedAction.setupRawHandler(new TypingBongoCatTypedActionHandler(this.originalHandler));
    }

    public static TypingBongoCat getInstance() {
        return ApplicationManager.getApplication().getService(TypingBongoCat.class);
    }

    @Override
    public void dispose() {
        // 서비스가 종료될 때, 가로챘던 핸들러를 원래대로 복구합니다.
        TypedAction.getInstance().setupRawHandler(originalHandler);
    }

    private static class TypingBongoCatTypedActionHandler implements TypedActionHandler {
        private final TypedActionHandler originalHandler;

        public TypingBongoCatTypedActionHandler(TypedActionHandler originalHandler) {
            this.originalHandler = originalHandler;
        }

        @Override
        public void execute(@NotNull final Editor editor, final char c, @NotNull final DataContext dataContext) {
            if (TypingBongoCat.getInstance().isEnabled()) {
                BongoCatController.getInstance().update(editor);
            }
            // null 체크를 추가하여 안정성을 높입니다.
            if (originalHandler != null) {
                originalHandler.execute(editor, c, dataContext);
            }
        }

        // --- 추가된 부분: 외부에서 원본 핸들러에 접근하기 위한 getter ---
        public TypedActionHandler getOriginalHandler() {
            return originalHandler;
        }
    }

    // --- PersistentStateComponent and Getters/Setters (unchanged) ---

    @Nullable
    @Override
    public TypingBongoCat getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull TypingBongoCat state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}