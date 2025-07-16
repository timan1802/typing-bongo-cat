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
import java.awt.Point;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.SwingUtilities;
import org.jetbrains.annotations.NotNull;


@Service(Service.Level.APP)
final class BongoCatController implements EditorFactoryListener, Disposable {

    private final Map<Editor, BongoCatAnimator> particleContainers = new ConcurrentHashMap<>();

    public BongoCatController() {
        EditorFactory.getInstance().addEditorFactoryListener(this, this);
    }

    public static BongoCatController getInstance() {
        return ApplicationManager.getApplication().getService(BongoCatController.class);
    }

    @Override
    public void editorCreated(@NotNull EditorFactoryEvent event) {
        particleContainers.computeIfAbsent(event.getEditor(), BongoCatAnimator::new);
    }

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

        VisualPosition visualPosition = editor.getCaretModel().getVisualPosition();
        Point point = editor.visualPositionToXY(visualPosition);
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