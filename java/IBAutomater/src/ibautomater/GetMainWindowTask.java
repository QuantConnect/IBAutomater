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

import java.awt.Window;
import java.util.concurrent.Callable;
import javax.swing.JMenuItem;

/**
 * Handles the task of finding the IBGateway main window and waiting for it to be ready.
 * 
 * @author QuantConnect Corporation
 */
public class GetMainWindowTask implements Callable<Window> {
    private final IBAutomater automater;

    /**
     * Creates a new instance of the {@link GetMainWindowTask} class.
     * 
     * @param automater The {@link IBAutomater} instance
     */
    GetMainWindowTask(IBAutomater automater) {
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

            for(Window w : Window.getWindows()) {
                JMenuItem menuItem = Common.getMenuItem(w, "Configure", "Settings");
                if (menuItem != null) {
                    this.automater.logMessage("Found main window (Window title: [" + Common.getTitle(w) + "] - Window name: [" + w.getName() + "])");

                    // when the main window is found and is ready,
                    // save it for future use and open the configuration window
                    this.automater.setMainWindow(w);
                    menuItem.doClick();

                    return w;
                }
            }

            this.automater.logMessage("Main window not found.");

            Thread.sleep(1000);
        }
    }
}
