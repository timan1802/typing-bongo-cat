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

    /**
     * 캐럿 위치가 에디터 제일 상단이거나, 제일 좌측일 경우 이미지가 안 나오는 것을 방지
     * @param editor
     * @return
     */
    private static @NotNull VisualPosition getVisualPosition(final Editor editor) {
        VisualPosition currentVisualPosition = editor.getCaretModel().getVisualPosition();
        int currentColumn = currentVisualPosition.getColumn();
        int currentLine = currentVisualPosition.getLine(); // 0부터 시작
        int replaceLine = currentLine;
        int replaceColumn = currentColumn;

        //7번째 단어까지는 오른쪽으로 좀더 이동해서 나오도록.
        if(currentColumn < 7 ){
            replaceColumn = 7;
        }

        //6번째 줄까지만 하단에 나오게.
        if(currentLine < 6 ){
            replaceLine = currentLine + 7;
        }

        return new VisualPosition(replaceLine, replaceColumn);
    }

    private void updateUI(Editor editor) {
        // SwingUtilities.invokeLater에 의해 코드가 실행되는 시점에는
        // 에디터가 이미 닫혔을 수 있으므로, isDisposed 체크를 추가합니다.
        if (editor.isDisposed()) {
            return;
        }

        final BongoCatAnimator bongoCatAnimator = particleContainers.computeIfAbsent(editor, BongoCatAnimator::new);

        VisualPosition newVisualPosition = getVisualPosition(editor);

        Point point = editor.visualPositionToXY(newVisualPosition);
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