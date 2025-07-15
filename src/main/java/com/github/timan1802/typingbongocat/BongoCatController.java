package com.github.timan1802.typingbongocat;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.ScrollingModel;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Service(Service.Level.APP)
final class BongoCatController implements EditorFactoryListener, Disposable {

    private final Map<Editor, BongoCatAnimator> particleContainers = new ConcurrentHashMap<>();

    public BongoCatController() {
        // 서비스가 생성될 때, 스스로를 EditorFactoryListener로 등록합니다.
        // 두 번째 인자 'this'는 이 서비스가 소멸될 때 리스너도 함께 제거되도록 합니다.
        EditorFactory.getInstance().addEditorFactoryListener(this, this);
    }

    public static BongoCatController getInstance() {
        return ApplicationManager.getApplication().getService(BongoCatController.class);
    }

    @Override
    public void editorCreated(@NotNull EditorFactoryEvent event) {
        // 에디터가 생성될 때 컨테이너를 생성하고 맵에 추가합니다.
        particleContainers.computeIfAbsent(event.getEditor(), BongoCatAnimator::new);
    }

    @Override
    public void editorReleased(@NotNull EditorFactoryEvent event) {
        // 에디터가 닫힐 때 컨테이너를 맵에서 제거하고, 리소스를 정리합니다.
        BongoCatAnimator container = particleContainers.remove(event.getEditor());
        if (container != null) {
            container.dispose(); // 매우 중요: 메모리 누수 방지
        }
    }

    public void update(final Editor editor) {
        if (TypingBongoCat.getInstance().isEnabled()) {
            SwingUtilities.invokeLater(() -> updateInUI(editor));
        }
    }

    private void updateInUI(Editor editor) {
        // 타이밍 문제 방지를 위해, 컨테이너가 없으면 이 시점에서 생성합니다.
        final BongoCatAnimator bongoCatAnimator = particleContainers.computeIfAbsent(editor, BongoCatAnimator::new);

        // 캐럿 위치는 더 이상 필요 없지만, 향후 확장을 위해 남겨둘 수 있습니다.
        VisualPosition visualPosition = editor.getCaretModel().getVisualPosition();
        Point point = editor.visualPositionToXY(visualPosition);
        ScrollingModel scrollingModel = editor.getScrollingModel();
        point.x -= scrollingModel.getHorizontalScrollOffset();
        point.y -= scrollingModel.getVerticalScrollOffset();

        // 계산된 위치를 update 메소드에 전달합니다.
        bongoCatAnimator.update(point);
    }

    @Override
    public void dispose() {
        // 서비스가 종료될 때, 모든 컨테이너의 리소스를 정리합니다.
        particleContainers.values().forEach(BongoCatAnimator::dispose);
        particleContainers.clear();
    }
}