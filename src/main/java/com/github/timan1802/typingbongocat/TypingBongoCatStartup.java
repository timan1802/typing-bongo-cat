package com.github.timan1802.typingbongocat;

import com.intellij.ide.AppLifecycleListener;
import com.intellij.openapi.application.ApplicationManager;
import java.util.List;
import org.jetbrains.annotations.NotNull;
//import test.CatMode;

/**
 * 애플리케이션 시작 시 KeyboardActionService를 초기화하는 리스너입니다.
 */
public class TypingBongoCatStartup implements AppLifecycleListener {

  @Override
  public void appFrameCreated(@NotNull List<String> commandLineArgs) {
    // 이 메소드는 IntelliJ의 메인 프레임이 생성된 직후 호출됩니다.
    // getService()를 호출하는 순간, KeyboardActionService의 인스턴스가 생성되고
    // 생성자 내부의 키보드 핸들러 등록 코드가 실행됩니다.
    ApplicationManager.getApplication().getService(TypingBongoCat.class);
  }
}