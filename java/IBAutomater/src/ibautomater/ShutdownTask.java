/*
 * QUANTCONNECT.COM - Democratizing Finance, Empowering Individuals.
 * IBAutomater v1.0. Copyright 2019 QuantConnect Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package ibautomater;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Callable;

/**
 * Handles the task of finding the IBGateway main window and shutting it down
 *
 * @author QuantConnect Corporation
 */
public class ShutdownTask implements Callable<Window> {
    private final IBAutomater automater;

    /**
     * Creates a new instance of the {@link ShutdownTask} class.
     *
     * @param automater The {@link IBAutomater} instance
     */
    ShutdownTask(IBAutomater automater) {

        this.automater = automater;
    }

    /**
     * Returns the IBGateway main window, or throws an exception if unable to do so.
     *
     * @return Returns the IBGateway main window
     */
    @Override
    @SuppressWarnings("SleepWhileInLoop")
    public Window call() throws Exception {
        while (true) {
            this.automater.logMessage("Finding main window...");

            for (Window w : Window.getWindows()) {
                JMenuItem menuItem = Common.getMenuItem(w, "File", "Close");
                if (menuItem != null) {
                    this.automater.logMessage("Found main window (Window title: [" + Common.getTitle(w) + "] - Window name: [" + w.getName() + "])");
                    menuItem.doClick();

                    return w;
                }
            }

            this.automater.logMessage("Main window not found.");

            Thread.sleep(1000);
        }
    }
}
