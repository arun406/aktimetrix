package com.aktimetrix.aktimetrix.reference.data.service;

import com.aktimetrix.aktimetrix.reference.data.dto.LineDTO;
import com.aktimetrix.aktimetrix.reference.data.model.Line;
import com.aktimetrix.aktimetrix.reference.data.repository.LinesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LineService {

    @Autowired
    private LinesRepository linesRepository;

    public void cleanup() {
        this.linesRepository.deleteAll();
    }

    public void saveAll(List<LineDTO> dtos) {
        List<Line> lines = dtos.stream()
                .map(dto -> {
                    Line line = new Line();
                    line.setAirline(dto.getAirline());
                    line.setFlightGroupCode(dto.getForwarderCode());
                    line.setAirport(dto.getAirport());
                    line.setExportInd(dto.getExportInd());
                    line.setImportInd(dto.getImportInd());
                    line.setTransitInd(dto.getTransitInd());
                    line.setProductCode(dto.getProductCode());
                    line.setProductGroupCode(dto.getProductGroupCode());
                    line.setDow(dto.getDow());
                    line.setFlightNo(dto.getFlightNo());
                    line.setFlightGroupCode(dto.getFlightGroupCode());
                    line.setAcCategory(dto.getAcCategory());
                    line.setFohBeforeFwbInd(dto.getFohBeforeFwbInd());
                    line.setFWB(dto.getFWB());
                    line.setLAT(dto.getLAT());
                    line.setRCS(dto.getRCS());
                    line.setDEP(dto.getDEP());
                    line.setARR(dto.getARR());
                    line.setAWR(dto.getAWR());
                    line.setRCF(dto.getRCF());
                    line.setNFD(dto.getNFD());
                    line.setAWD(dto.getAWD());
                    line.setDLV(dto.getDLV());
                    line.setRCFT(dto.getRCFT());
                    line.setTFDT(dto.getTFDT());
                    line.setARRT(dto.getARRT());
                    line.setRCTT(dto.getRCTT());
                    line.setDEPT(dto.getDEPT());
                    line.setWtInd(dto.getWtInd());
                    line.setVolInd(dto.getVolInd());
                    line.setEFreightInd(dto.getEFreightInd());
                    line.setNotes(dto.getNotes());
                    return line;
                })
                .collect(Collectors.toList());
        this.linesRepository.saveAll(lines);
    }

    public void save(LineDTO dto) {
        Line line = new Line();
        line.setAirline(dto.getAirline());
        line.setFlightGroupCode(dto.getForwarderCode());
        line.setAirline(dto.getAirport());
        line.setExportInd(dto.getExportInd());
        line.setImportInd(dto.getImportInd());
        line.setTransitInd(dto.getTransitInd());
        line.setProductCode(dto.getProductCode());
        line.setProductGroupCode(dto.getProductGroupCode());
        line.setDow(dto.getDow());
        line.setFlightNo(dto.getFlightNo());
        line.setFlightGroupCode(dto.getFlightGroupCode());
        line.setAcCategory(dto.getAcCategory());
        line.setFohBeforeFwbInd(dto.getFohBeforeFwbInd());
        line.setFWB(dto.getFWB());
        line.setLAT(dto.getLAT());
        line.setRCS(dto.getRCS());
        line.setDEP(dto.getDEP());
        line.setARR(dto.getARR());
        line.setAWR(dto.getAWR());
        line.setRCF(dto.getRCF());
        line.setNFD(dto.getNFD());
        line.setAWD(dto.getAWD());
        line.setDLV(dto.getDLV());
        line.setRCFT(dto.getRCFT());
        line.setTFDT(dto.getTFDT());
        line.setARRT(dto.getARRT());
        line.setRCTT(dto.getRCTT());
        line.setDEPT(dto.getDEPT());
        line.setWtInd(dto.getWtInd());
        line.setVolInd(dto.getVolInd());
        line.setEFreightInd(dto.getEFreightInd());
        line.setNotes(dto.getNotes());
        this.linesRepository.save(line);
    }

    public List<LineDTO> list() {
        List<Line> all = this.linesRepository.findAll();
        if (all == null) {
            return new ArrayList<>();
        }
        return all.stream()
                .map(m -> {
                    LineDTO dto = new LineDTO();
                    dto.setAirline(m.getAirline());
                    dto.setFlightGroupCode(m.getForwarderCode());
                    dto.setAirline(m.getAirport());
                    dto.setExportInd(m.getExportInd());
                    dto.setImportInd(m.getImportInd());
                    dto.setTransitInd(m.getTransitInd());
                    dto.setProductCode(m.getProductCode());
                    dto.setProductGroupCode(m.getProductGroupCode());
                    dto.setDow(m.getDow());
                    dto.setFlightNo(m.getFlightNo());
                    dto.setFlightGroupCode(m.getFlightGroupCode());
                    dto.setAcCategory(m.getAcCategory());
                    dto.setFohBeforeFwbInd(m.getFohBeforeFwbInd());
                    dto.setFWB(m.getFWB());
                    dto.setLAT(m.getLAT());
                    dto.setRCS(m.getRCS());
                    dto.setDEP(m.getDEP());
                    dto.setARR(m.getARR());
                    dto.setAWR(m.getAWR());
                    dto.setRCF(m.getRCF());
                    dto.setNFD(m.getNFD());
                    dto.setAWD(m.getAWD());
                    dto.setDLV(m.getDLV());
                    dto.setRCFT(m.getRCFT());
                    dto.setTFDT(m.getTFDT());
                    dto.setARRT(m.getARRT());
                    dto.setRCTT(m.getRCTT());
                    dto.setDEPT(m.getDEPT());
                    dto.setWtInd(m.getWtInd());
                    dto.setVolInd(m.getVolInd());
                    dto.setEFreightInd(m.getEFreightInd());
                    dto.setNotes(m.getNotes());
                    return dto;
                })
                .collect(Collectors.toList());
    }

}
