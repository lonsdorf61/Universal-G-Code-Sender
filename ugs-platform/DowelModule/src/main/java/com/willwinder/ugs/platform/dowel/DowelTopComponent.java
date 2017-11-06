/*
    Copyright 2017 Will Winder

    This file is part of Universal Gcode Sender (UGS).

    UGS is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    UGS is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with UGS.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.willwinder.ugs.platform.dowel;

import static com.willwinder.universalgcodesender.utils.SwingHelpers.getDouble;
import static com.willwinder.universalgcodesender.utils.SwingHelpers.getInt;
import static com.willwinder.universalgcodesender.utils.SwingHelpers.selectedUnit;
import static com.willwinder.universalgcodesender.utils.SwingHelpers.unitIdx;

import com.google.gson.Gson;
import com.willwinder.ugs.nbm.visualizer.shared.RenderableUtils;
import com.willwinder.ugs.nbp.lib.lookup.CentralLookup;
import com.willwinder.ugs.nbp.lib.services.LocalizingService;
import com.willwinder.ugs.platform.dowel.renderable.DowelPreview;
import com.willwinder.universalgcodesender.listeners.UGSEventListener;
import com.willwinder.universalgcodesender.model.BackendAPI;
import com.willwinder.universalgcodesender.utils.SwingHelpers;

import net.miginfocom.swing.MigLayout;

import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.windows.TopComponent;

import java.awt.*;

import javax.swing.*;
import javax.swing.border.Border;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(
        dtd = "-//com.willwinder.ugs.platform.dowel//Dowel//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "DowelTopComponent",
        //iconBase="SET/PATH/TO/ICON/HERE",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "output", openAtStartup = false)
@ActionID(
        category = "Window",
        id = LocalizingService.DowelActionId)
@ActionReference(path = LocalizingService.PLUGIN_WINDOW)
@TopComponent.OpenActionRegistration(
        displayName = "Dowel",
        preferredID = "DowelTopComponent"
)
public final class DowelTopComponent extends TopComponent implements UGSEventListener {
  final static String JSON_PROPERTY = "dowel_settings_json";

  private final BackendAPI backend;

  private final SpinnerNumberModel numDowelsX;
  private final SpinnerNumberModel numDowelsY;
  private final SpinnerNumberModel dowelDiameter;
  private final SpinnerNumberModel dowelLength;
  private final SpinnerNumberModel bitDiameter;
  private final SpinnerNumberModel feed;
  private final SpinnerNumberModel cutDepth;
  private final JComboBox<String> units;

  private static final Gson GSON = new Gson();

  private final DowelGenerator generator;
  private final DowelPreview preview;

  public DowelTopComponent() {
    setName(LocalizingService.DowelTitle);
    setToolTipText(LocalizingService.DowelTooltip);

    backend = CentralLookup.getDefault().lookup(BackendAPI.class);
    backend.addUGSEventListener(this);

    double doubleSpinner = 1000000;
    int intSpinner = 1000000;

    numDowelsX = new SpinnerNumberModel(10, -intSpinner, intSpinner, 1);
    numDowelsY = new SpinnerNumberModel(10, -intSpinner, intSpinner, 1);
    dowelDiameter = new SpinnerNumberModel(10, -doubleSpinner, doubleSpinner, 1);
    dowelLength = new SpinnerNumberModel(10, -doubleSpinner, doubleSpinner, 1);
    bitDiameter = new SpinnerNumberModel(10, -doubleSpinner, doubleSpinner, 1);
    feed = new SpinnerNumberModel(10, -doubleSpinner, doubleSpinner, 1);
    cutDepth = new SpinnerNumberModel(10, -doubleSpinner, doubleSpinner, 1);

    units = new JComboBox<>(SwingHelpers.getUnitOptions());

    generator = new DowelGenerator(getSettings());
    preview = new DowelPreview("Dowel Preview", generator);

    // Change listener...
    numDowelsX.addChangeListener(l -> controlChangeListener());
    numDowelsY.addChangeListener(l -> controlChangeListener());
    dowelDiameter.addChangeListener(l -> controlChangeListener());
    dowelLength.addChangeListener(l -> controlChangeListener());
    bitDiameter.addChangeListener(l -> controlChangeListener());
    feed.addChangeListener(l -> controlChangeListener());
    cutDepth.addChangeListener(l -> controlChangeListener());
    units.addActionListener(l -> controlChangeListener());

    Border blackline = BorderFactory.createLineBorder(Color.black);

    // Dowel settings
    JPanel dowelPanel = new JPanel();
    dowelPanel.setBorder(BorderFactory.createTitledBorder(blackline, "Dowel"));
    dowelPanel.setLayout(new MigLayout("fillx, wrap 4"));

    dowelPanel.add(new JLabel("X Count"), "growx");
    dowelPanel.add(new JSpinner(numDowelsX), "growx");

    dowelPanel.add(new JLabel("Diameter"), "growx");
    dowelPanel.add(new JSpinner(dowelDiameter), "growx");

    dowelPanel.add(new JLabel("Y Count"), "growx");
    dowelPanel.add(new JSpinner(numDowelsY), "growx");

    dowelPanel.add(new JLabel("Length"), "growx");
    dowelPanel.add(new JSpinner(dowelLength), "growx");

    // Gcode settings
    JPanel cutPanel = new JPanel();
    cutPanel.setBorder(BorderFactory.createTitledBorder(blackline, "Settings"));
    cutPanel.setLayout(new MigLayout("fillx, wrap 4"));

    cutPanel.add(new JLabel("Units"), "growx");
    cutPanel.add(units, "growx");

    cutPanel.add(new JLabel("Feed rate"), "growx");
    cutPanel.add(new JSpinner(feed), "growx");

    cutPanel.add(new JLabel("Bit diameter"), "growx");
    cutPanel.add(new JSpinner(bitDiameter), "growx");

    cutPanel.add(new JLabel("Cut depth"), "growx");
    cutPanel.add(new JSpinner(cutDepth), "growx");

    // Put it all together
    setLayout(new MigLayout("fillx, wrap 2"));
    add(dowelPanel, "grow");
    add(cutPanel, "grow");
  }

  private void controlChangeListener() {
    this.generator.setSettings(getSettings());
  }

  public DowelSettings getSettings() {
    return new DowelSettings(
        getInt(this.numDowelsX),
        getInt(this.numDowelsY),
        getDouble(this.dowelDiameter),
        getDouble(this.dowelLength),
        getDouble(this.bitDiameter),
        getDouble(this.feed),
        getDouble(this.cutDepth),
        selectedUnit(this.units.getSelectedIndex()));
  }

  @Override
  public void UGSEvent(com.willwinder.universalgcodesender.model.UGSEvent evt) {
  }

  @Override
  public void componentOpened() {
    RenderableUtils.registerRenderable(preview);
  }

  @Override
  public void componentClosed() {
    RenderableUtils.removeRenderable(preview);
  }

  void writeProperties(java.util.Properties p) {
    // better to version settings since initial version as advocated at
    // http://wiki.apidesign.org/wiki/PropertyFiles
    p.setProperty("version", "1.0");
    p.setProperty(JSON_PROPERTY, GSON.toJson(getSettings()));
  }

  void readProperties(java.util.Properties p) {
    String version = p.getProperty("version");

    if (p.containsKey(JSON_PROPERTY)) {
      String json = p.getProperty(JSON_PROPERTY);
      DowelSettings ds = new Gson().fromJson(json, DowelSettings.class);

      this.numDowelsX.setValue(ds.getNumDowelsX());
      this.numDowelsY.setValue(ds.getNumDowelsY());
      this.dowelDiameter.setValue(ds.getDowelDiameter());
      this.dowelLength.setValue(ds.getDowelLength());
      this.bitDiameter.setValue(ds.getBitDiameter());
      this.feed.setValue(ds.getFeed());
      this.cutDepth.setValue(ds.getCutDepth());
      this.units.setSelectedIndex(unitIdx(ds.getUnits()));
    }
  }
}