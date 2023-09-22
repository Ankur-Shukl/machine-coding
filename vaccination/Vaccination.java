import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Vaccination center- multiple vaccine types viz. Covishield, Covaxin, Sputnik, etc
 * 
 * Vaccine availability ata center is defined as the number of vaccines available, 
 * number of vaccines booked, vaccine type and dose type.
 * 
 * Add a vaccine center in the system.
Add availability of vaccines in the center based on type of vaccine and type of dose.
Update avalablity of vaccines in the center based on type of vaccine and type of dose.
Remove avallability of vaccines in the center based on type of vaccine and type of dose.
Book a vaccine slot, given a center id, type of vaccine and type of dose.
Search vaccine centers by below filter predicates
Search by vaccine type and dose type (Ex: Search for centers which have Covishield dose1 available)

Model- Vaccinaton Center, Vaccine Type, Dose type,
Vaccine center(Name, Location(Pincode, Address Line1, Address line 2, City), Vaccines, Slot(Date, Start time, end time), Description, Tags(pvt Hospital, PHC)) 

Vaccine(Type, Dose type)
VaccineInventory (ConcurrentHashMap<Vaccine, count>)

User(Name, phone no, UID, Vaccination History(Vaccine, Slot, vaccine center name))

 */

class InvalidInputException extends Exception {
    public InvalidInputException(String message) {
        super(message);
    }

}
class VaccinationCenterService {
    private static Map<String, VaccineCenter> vaccineCenters = new ConcurrentHashMap<>();

    public VaccinationCenterService() {

    }

    public boolean add(VaccineCenter vaccineCenter) {
        VaccineCenter addedCenter = vaccineCenters.putIfAbsent(vaccineCenter.getId(), vaccineCenter);
        if(addedCenter != null) {
            return false;
        }
        return true;
    }

    boolean addAvailability(String vaccineCenterId, VaccineAvailability vaccineAvailability) {
        VaccineCenter center = vaccineCenters.get(vaccineCenterId);
        if(center == null) {
            return false;
        }
        return center.addVaccine(vaccineAvailability.vaccine, vaccineAvailability);
    }

    public boolean updateAvailability(String vaccineCenterId, VaccineAvailability vaccineAvailability) {
        VaccineCenter center = vaccineCenters.get(vaccineCenterId);
        if(center == null) {
            return false;
        }
        return center.updateVaccineAvailability(vaccineAvailability);
    }

    boolean removeAvailability(String vaccineCenterId, VaccineAvailability vaccineAvailability) {
        VaccineCenter center = vaccineCenters.get(vaccineCenterId);
        if(center == null) {
            return false;
        }
        return center.removeVaccineAvailability(vaccineAvailability);

    }


    boolean bookVaccineSlot(String vaccineCenterId, VaccineType vaccineType, VaccineDoseType doseType){
        VaccineCenter center = vaccineCenters.get(vaccineCenterId);
        if(center == null) {
            return false;
        }
        return center.bookVaccinationSlot(vaccineType, doseType);
    }

    public SearchResponse search(VaccineType vaccineType, VaccineDoseType doseType) {
        List<VaccineCenter> availableCentersWithVaccine = new LinkedList<>();
        for(Map.Entry<String, VaccineCenter> center: vaccineCenters.entrySet()) {
            if(center.getValue().hasVaccines(vaccineType, doseType)) {
                availableCentersWithVaccine.add(center.getValue());
            }
        }
        return new SearchResponse(availableCentersWithVaccine.size(), availableCentersWithVaccine);
    }

    public SearchResponse search(List searchRequestList) {
        Set<VaccineCenter> availableCentersWithVaccine = new HashSet<>();

        for(Object req: searchRequestList) {
            SearchRequest toSearch = ((SearchRequest) req);
            for(Map.Entry<String, VaccineCenter> center: vaccineCenters.entrySet()) {
                if(center.getValue().hasVaccines(toSearch.getVaccineType(), toSearch.getDoseType())) {
                    availableCentersWithVaccine.add(center.getValue());
                }
            }
        }
        return new SearchResponse(availableCentersWithVaccine.size(), new ArrayList<>(availableCentersWithVaccine));
    }


}

class SearchRequest {
    VaccineType vaccineType;
    VaccineDoseType doseType;

    public SearchRequest(VaccineType vaccineType, VaccineDoseType doseType) {
        this.vaccineType = vaccineType;
        this.doseType = doseType;
    }

    public VaccineType getVaccineType() {
        return vaccineType;
    }

    public VaccineDoseType getDoseType() {
        return doseType;
    }
}

class SearchResponse {
    private int count;
    private List<VaccineCenter> centers;

    public SearchResponse(int count, List<VaccineCenter> centers) {
        this.count = count;
        this.centers = centers;
    }

    public int getCount() {
        return count;
    }

    public List<VaccineCenter> getCenters() {
        return this.centers;
    }
}

class VaccineAvailability {
    Vaccine vaccine;
    Integer dayAvailability;

    public VaccineAvailability(Vaccine vaccine, Integer dayAvailability) {
        this.vaccine = vaccine;
        this.dayAvailability = dayAvailability;
    }

    public Integer getDayAvailability() {
        return this.dayAvailability;
    }
}

class VaccineCenter {
    private String id;
    private String name;
    private Location location;    
    private String description;
    private Set<Tag> tags;    
    private Map<String, Integer> vaccineInventory = new ConcurrentHashMap();

    public String getId() {
        return this.id;
    }

    private String getVaccineKey(Vaccine vaccine) {
        return vaccine.GetVaccineType()+"-"+vaccine.GetDoseType();
    }

    public VaccineCenter(String id, String name, Location location, Optional<String> description, Optional<Set<Tag>> tags) {
        this.id = id;
        this.name = name;
        this.location = location;
        description.ifPresent(d -> this.description = d);
        tags.ifPresent(t -> this.tags = t);
    }

    public boolean addVaccine(Vaccine vaccine, VaccineAvailability availability) {
        if(vaccineInventory.containsKey(getVaccineKey(vaccine))) {
            return false;
        }
        vaccineInventory.put(getVaccineKey(vaccine), availability.getDayAvailability());
        return true;
    }

    public boolean updateVaccineAvailability(VaccineAvailability newVaccineAvailability) {
        if(!vaccineInventory.containsKey(getVaccineKey(newVaccineAvailability.vaccine))) {
            return false;
        }
        vaccineInventory.put(getVaccineKey(newVaccineAvailability.vaccine),
                vaccineInventory.get(getVaccineKey(newVaccineAvailability.vaccine))+newVaccineAvailability.getDayAvailability());
        return true;
    }

    public boolean removeVaccineAvailability(VaccineAvailability vaccineAvailability) {
        if(vaccineInventory.remove(getVaccineKey(vaccineAvailability.vaccine)) == null) {
            return false;
        }
        return true;
    }

    public boolean bookVaccinationSlot(VaccineType vaccineType, VaccineDoseType doseType) {
        String key = getVaccineKey(new Vaccine(vaccineType, doseType));
        Integer availableCount = vaccineInventory.get(key);
        if(availableCount <= 0) {
            return false;
        }
        vaccineInventory.put(key, availableCount-1);
        return true;
    }

    public boolean hasVaccines(VaccineType vType, VaccineDoseType dType) {
        Integer count = vaccineInventory.get(getVaccineKey(new Vaccine(vType, dType)));
        return  count != null && count > 0;
    }

}

enum VaccineType {
    COVISHIELD,
    CAVAXIN,
}

enum VaccineDoseType {
    DOSE1,
    DOSE2,
}


class Vaccine {
    private VaccineType vaccineType;
    private VaccineDoseType doseType;

    public Vaccine(VaccineType vType, VaccineDoseType dType) {
        this.vaccineType = vType;
        this.doseType = dType;
    }

    public VaccineType GetVaccineType() {
        return this.vaccineType;
    }

    public VaccineDoseType GetDoseType() {
        return this.doseType;
    }
}

enum Tag {
    PHC,
    PVT_HOSPITAL,
    SCHOOL,    
}

class Constants {
    public static final int slotLength = 2;
}

//class Slot {
//    private Date startTime;
//    private Date endTime;
//
//    public Slot(Date time) {
//        this.startTime = time;
//        this.endTime = time + slotLength;
//    }
//}

class Location {
    private int pincode;
    private String addressLine1;
    private String addressLine2;
    private String city;
    
    public Location(int pincode, String addressLine1, String addressLine2, String city) {
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.city = city;
        this.pincode = pincode;
    }
    
}

class Vaccination {
    public static void main(String... args) {
        VaccinationCenterService service = new VaccinationCenterService();
        VaccineCenter vc1 = new VaccineCenter("C1", "Center1", new Location(208080, "ABCD1", "PQRS2", "Pune"),
                Optional.of(
                "PHC type 1"), Optional.empty());
        service.add(vc1);
        SearchResponse response = service.search(VaccineType.COVISHIELD, VaccineDoseType.DOSE1);
        System.out.println("first run");
        response.getCenters().forEach(System.out::println);
        service.addAvailability("C1", new VaccineAvailability(new Vaccine(VaccineType.COVISHIELD,
                VaccineDoseType.DOSE1), 10));
        response = service.search(VaccineType.COVISHIELD, VaccineDoseType.DOSE1);
        System.out.println("second search");
        response.getCenters().forEach(System.out::println);

        service.addAvailability("C1", new VaccineAvailability(new Vaccine(VaccineType.COVISHIELD,
                VaccineDoseType.DOSE2), 10));
        response = service.search(VaccineType.COVISHIELD, VaccineDoseType.DOSE2);
        System.out.println("third search");
        response.getCenters().forEach(System.out::println);
        response = service.search(VaccineType.CAVAXIN, VaccineDoseType.DOSE2);
        System.out.println("fouth search");
        response.getCenters().forEach(System.out::println);
        VaccineCenter vc2 = new VaccineCenter("C2", "Center1", new Location(208080, "ABCD1", "PQRS2", "Pune"),
                Optional.of(
                        "PHC type 1"), Optional.empty());
        service.add(vc2);
        service.addAvailability("C2", new VaccineAvailability(new Vaccine(VaccineType.CAVAXIN,
                VaccineDoseType.DOSE1), 10));
        response = service.search(VaccineType.CAVAXIN, VaccineDoseType.DOSE2);
        System.out.println("fifth search");
        response.getCenters().forEach(System.out::println);
        response = service.search(VaccineType.CAVAXIN, VaccineDoseType.DOSE1);
        System.out.println("sixth search");
        response.getCenters().forEach(System.out::println);


        System.out.println("removing covishield dose 1:"+ service.removeAvailability("C1",
                new VaccineAvailability(new
                        Vaccine(VaccineType.COVISHIELD,
                        VaccineDoseType.DOSE2), 0)));
        System.out.println("removing covishield dose 1:"+ service.removeAvailability("C1",
                new VaccineAvailability(new
                        Vaccine(VaccineType.CAVAXIN,
                        VaccineDoseType.DOSE2), 0)));
        response = service.search(VaccineType.COVISHIELD, VaccineDoseType.DOSE2);
        System.out.println("seventh search");
        response.getCenters().forEach(System.out::println);

    }
}