package com.github.timan1802.typingbongocat;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.ScrollingModel;
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
        EditorFactory.getInstance().addEditorFactoryListener(this, this);
    }

    public static BongoCatController getInstance() {
        return ApplicationManager.getApplication().getService(BongoCatController.class);
    }

    // editorCreated 는 구현하지 않습니다. 애니메이터는 실제로 타이핑이 일어난 에디터에만
    // updateUI() 에서 지연 생성합니다. (diff·콘솔·미리보기 등 모든 에디터에 붙이지 않기 위함)

    @Override
    public void editorReleased(@NotNull EditorFactoryEvent event) {
        BongoCatAnimator container = particleContainers.remove(event.getEditor());
        if (container != null) {
            container.dispose();
        }
    }

    public void update(final Editor editor) {
        SwingUtilities.invokeLater(() -> updateUI(editor));
    }

    private void updateUI(Editor editor) {
        // SwingUtilities.invokeLater에 의해 코드가 실행되는 시점에는
        // 에디터가 이미 닫혔을 수 있으므로, isDisposed 체크를 추가합니다.
        if (editor.isDisposed()) {
            return;
        }

        final BongoCatAnimator bongoCatAnimator = particleContainers.computeIfAbsent(editor, BongoCatAnimator::new);

        // 뷰포트 경계에서 잘리지 않도록 하는 위치 보정은 BongoCatAnimator 가 그릴 때 처리합니다.
        Point point = editor.visualPositionToXY(editor.getCaretModel().getVisualPosition());
        ScrollingModel scrollingModel = editor.getScrollingModel();
        point.x -= scrollingModel.getHorizontalScrollOffset();
        point.y -= scrollingModel.getVerticalScrollOffset();

        bongoCatAnimator.update(point);
    }

    @Override
    public void dispose() {
        particleContainers.values().forEach(BongoCatAnimator::dispose);
        particleContainers.clear();
    }
}