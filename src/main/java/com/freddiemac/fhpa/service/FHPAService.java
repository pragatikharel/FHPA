package com.freddiemac.fhpa.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.stereotype.Component;

import com.freddiemac.fhpa.model.HPIPOMonthlyHist;
import com.freddiemac.fhpa.model.LongerHpiExpUsNsa;

import jakarta.annotation.PostConstruct;

@Component
public class FHPAService {

	private int qtr_no;
	private String add_qtr;
	
	private LocalDate lamaDate = LocalDate.now();
	private LocalDate hpiQtr = LocalDate.now();
	private int month = 9;
	private List<Integer> quarters = Arrays.asList(3,6,9);
	
	@PostConstruct
    public void init() {
//        LOG.info(Arrays.asList(environment.getDefaultProfiles()));
    }
	
	@PostConstruct
	public void process() {
		System.out.println("Process Started");
		List<LongerHpiExpUsNsa> resultMapper = prepareTheData();
		for(int i=0;i<resultMapper.size();i++) {
			LongerHpiExpUsNsa result = resultMapper.get(i);
			int cutOffMonth = hpiQtr.getMonthValue();
			int quarter = result.getQuarter();
			int year = result.getYear();
			if(result.getPlace().equalsIgnoreCase("USD") && result.getYear() >= 1991) {
				qtr_no = 0;
				add_qtr = "N";
			}
			if(month != 3 || month != 6 || month != 9 || month != 12) {
				if(quarters.contains(cutOffMonth)){
					quarter = quarter+1;
					qtr_no = qtr_no+1;
				}else if(cutOffMonth == 12) {
					quarter = result.getQuarter()+1;
					qtr_no = qtr_no+1;
					year = year+1;
				}
				add_qtr = "Y";
			}
			double lhpi=1;
			double hpi3;
			double hpi2;
			double hpi1;
			
			double qtr_no1;
			double qtr_no2;
			double qtr_no3;
			
			LocalDate lhpi1_qtr;
			LocalDate lhpi2_qtr;
			if(i > 0) {
				lhpi = resultMapper.get(i-1).getHpi();
//				lhpi1_qtr = resultMapper.get(i-1).getQuarter();
			}
			
			if(i > 1) {
//				lhpi2_qtr = resultMapper.get(i-2).getQuarter();
			}
			if(result.getYear() == 1991 && quarter == 1) {
				hpi3 = result.getHpi();
				hpi2 = lhpi*((i/lhpi)*(2/3));
				hpi1 = lhpi*((i/lhpi)*(1/3));
				
				qtr_no1 = (qtr_no - 1) + (qtr_no - (qtr_no-1))/3;
				qtr_no2 = (qtr_no - 1) + 2 *(qtr_no - (qtr_no-1))/3;
				qtr_no3 = qtr_no;
			}
			
			int month1 = 1+3*(quarter-1);
			int month2 = 2+3*(quarter-1);
			int month3 = 3+3*(quarter-1);
			
			if(result.getYear() == 1991 && month == 1) {
				lhpi1_qtr = hpiQtr;
			}
			
			if(result.getYear() == 1991 && (month == 1 || month ==2)) {
				lhpi2_qtr = hpiQtr;
			}
			
			List<HPIPOMonthlyHist> montlyHistResults = processHPIPOMonthlyHistFile();
			calculateGrowthRate(montlyHistResults);
			
		}
	}
	
	public List<LongerHpiExpUsNsa> prepareTheData() {
		List<LongerHpiExpUsNsa> resultMappers = new ArrayList<>();
		FileInputStream resultFileStream=null;
		HSSFWorkbook wb=null; 
		try {
			resultFileStream=new FileInputStream(new File("/fmacdata/utility/carrac/euc_dev/ccf/final_rule/assumptions/cc_adj/202109/input./longer_HPI_EXP_us_nsa.xls"));
			wb = new HSSFWorkbook(resultFileStream);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
		
	    HSSFSheet sheet=wb.getSheet("longer_HPI_EXP_us_nsa");
	    
	    int rowIndex=0;
	    Row row;
	    Iterator<Row> rowIterator = sheet.iterator();
	    while (rowIterator.hasNext()) {
	    	if(rowIndex > 4) {
	    		row = rowIterator.next();
	    		LongerHpiExpUsNsa rowMapper = prepareRowMapper(row);
	    		resultMappers.add(rowMapper);
	    		rowIndex++;
	    	}else {
	    		row = rowIterator.next();
	    		rowIndex++;
	    	}
		      
	      }  
        return resultMappers;
	}
	
	/**
     * this method will process the row 
     * @param row
     * @return
     */
    private LongerHpiExpUsNsa prepareRowMapper(Row row) {
    	LongerHpiExpUsNsa rowMapper = new LongerHpiExpUsNsa();
		int cellIndex =0;
		for(Cell cell: row) { 
			processCellValue(cellIndex,rowMapper,cell);
			 if(cellIndex == 4) {
				break; 
			 }
			cellIndex++;
		}
	 return rowMapper;
    }
    
    /**
     * This method will process the cells of the row
     * @param cellindex
     * @param rowMapper
     * @param cell
     */
    private void processCellValue(int cellindex,LongerHpiExpUsNsa rowMapper,Cell cell){
    	switch(cellindex) {
	    	case 0:
	    		rowMapper.setPlace(cell.getStringCellValue());
	    		break;
	    	case 1:
	    		rowMapper.setYear((int)cell.getNumericCellValue());
	    		break;
	    	case 2:
	    		rowMapper.setQuarter((int)cell.getNumericCellValue());
	    		break;
	    	case 3:
				rowMapper.setHpi(cell.getNumericCellValue());
    		    break;
    	}	
    }
    
    public List<HPIPOMonthlyHist> processHPIPOMonthlyHistFile() {
		List<HPIPOMonthlyHist> resultMappers = new ArrayList<>();
		FileInputStream resultFileStream=null;
		HSSFWorkbook wb=null; 
		try {
			resultFileStream=new FileInputStream(new File("/fmacdata/utility/carrac/euc_dev/ccf/final_rule/assumptions/cc_adj/202109/input./HPI_PO_monthly_hist.xls"));
			wb = new HSSFWorkbook(resultFileStream);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
		
	    HSSFSheet sheet=wb.getSheet("HPI_PO_monthly_hist");
	    
	    int rowIndex=0;
	    Row row;
	    Iterator<Row> rowIterator = sheet.iterator();
	    while (rowIterator.hasNext()) {
	    	if(rowIndex > 3) {
	    		row = rowIterator.next();
	    		HPIPOMonthlyHist rowMapper = prepareHPIPORowMapper(row);
	    		resultMappers.add(rowMapper);
	    		rowIndex++;
	    	}else {
	    		row = rowIterator.next();
	    		rowIndex++;
	    	}
		      
	      }  
        return resultMappers;
	}
    
    /**
     * this method will process the row 
     * @param row
     * @return
     */
    private HPIPOMonthlyHist prepareHPIPORowMapper(Row row) {
    	HPIPOMonthlyHist rowMapper = new HPIPOMonthlyHist();
		int cellIndex =0;
		for(Cell cell: row) { 
			processCellValue(cellIndex,rowMapper,cell);
			 if(cellIndex == 18) {
				break; 
			 }
			cellIndex++;
		}
	 return rowMapper;
    }
    
    /**
     * This method will process the cells of the row
     * @param cellindex
     * @param rowMapper
     * @param cell
     */
    private void processCellValue(int cellindex,HPIPOMonthlyHist rowMapper,Cell cell){
    	switch(cellindex) {
	    	case 0:
	    		rowMapper.setMonth(cell.getLocalDateTimeCellValue().toLocalDate());
	    		break;
	    	case 1:
	    		rowMapper.setEastSouthCentralSA(cell.getNumericCellValue());
	    		break;
	    	case 2:
	    		rowMapper.setMiddleAtlanticNSA(cell.getNumericCellValue());
	    		break;
	    	case 3:
	    		rowMapper.setMiddleAtlanticSA(cell.getNumericCellValue());
    		    break;
	    	case 4:
	    		rowMapper.setMountainNSA(cell.getNumericCellValue());
	    		break;
	    	case 5:
	    		rowMapper.setMountainSA(cell.getNumericCellValue());
	    		break;
	    	case 6:
	    		rowMapper.setNewEnglandNSA(cell.getNumericCellValue());
	    		break;
	    	case 7:
	    		rowMapper.setNewEnglandSA(cell.getNumericCellValue());
    		    break;
	    	case 8:
	    		rowMapper.setPacificNSA(cell.getNumericCellValue());
	    		break;
	    	case 9:
	    		rowMapper.setPacificSA(cell.getNumericCellValue());
	    		break;
	    	case 10:
	    		rowMapper.setSouthAtlanticNSA(cell.getNumericCellValue());
	    		break;
	    	case 11:
	    		rowMapper.setSouthAtlanticSA(cell.getNumericCellValue());
    		    break;
	    	case 12:
	    		rowMapper.setWestNorthCentralNSA(cell.getNumericCellValue());
	    		break;
	    	case 13:
	    		rowMapper.setWestNorthCentralSA(cell.getNumericCellValue());
    		    break;
	    	case 14:
	    		rowMapper.setWestSouthCentralSA(cell.getNumericCellValue());
	    		break;
	    	case 15:
	    		rowMapper.setWestSouthCentralSA(cell.getNumericCellValue());
	    		break;
	    	case 16:
	    		rowMapper.setUsaNSA(cell.getNumericCellValue());
	    		break;
	    	case 17:
	    		rowMapper.setUsaSA(cell.getNumericCellValue());
    		    break;
    	}	
    }
    
    public void calculateGrowthRate(List<HPIPOMonthlyHist> hpiPoMonthlyHistList) {
    	for(int i=0;i<hpiPoMonthlyHistList.size();i++) {
    		HPIPOMonthlyHist monthlyHist = hpiPoMonthlyHistList.get(i);
    		LocalDate mon = monthlyHist.getMonth();
    		
    		String startDateString = "01Jan1991";
    		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMMyyyy", Locale.ENGLISH);
    		LocalDate startDate = LocalDate.parse(startDateString, formatter);
    		
    		String endDateString = "01Mar1991";
    		LocalDate endDate = LocalDate.parse(endDateString, formatter);
    		double lhpi1=0.0;
    		double lhpi2=0.0;
    		if(mon.compareTo(startDate) == 0) {
    			lhpi1 = monthlyHist.getUsaNSA();
    		}
    		if(mon.compareTo(endDate) < 0) {
    			lhpi2 = monthlyHist.getUsaNSA();
    		}
    		
    		double gr_rt_mo1 = monthlyHist.getUsaNSA()/lhpi1;
    		double gr_rt_mo2 = monthlyHist.getUsaNSA()/lhpi2;
    		
//    		double 
    	}
    }
}
