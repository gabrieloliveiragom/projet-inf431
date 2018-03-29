import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedList;

public class Particle implements Runnable {
	
	public static final double DELTAT = 0.001;
	//public static final double G_CONST = 6.67408E-11;
	public static final double G_CONST = 1.0;
	
	private Vector2D position, new_position;
	private Vector2D velocity, new_velocity;
	private double mass;
	private double r = 10;
	private LinkedList<Vector2D> forces;
	private ArrayList<Particle> particles;
	private ParticleSystem system;
	
	// Constructors
	public Particle() {
		this.mass = 1.0E5;
		this.position = new Vector2D(true, Display.SIZE);
		this.velocity = new Vector2D();
		this.forces = new LinkedList<Vector2D>();
	}
	
	public Particle(boolean randomVelocity) {
		this.mass = 1.0E5;
		this.position = new Vector2D(true, Display.SIZE);
		this.velocity = new Vector2D(randomVelocity, 5);
		this.forces = new LinkedList<Vector2D>();
	}
	
	public Particle(Vector2D position) {
		this.mass = 1.0E5;
		this.position = position;
		this.velocity = new Vector2D();
		this.forces = new LinkedList<Vector2D>();
	}
	
	public Particle(double mass, Vector2D position) {
		this.mass = mass;
		this.position = position;
		this.velocity = new Vector2D();
		this.forces = new LinkedList<Vector2D>();
	}
	
	public Particle(double mass, Vector2D position, Vector2D velocity) {
		this.mass = mass;
		this.position = position;
		this.velocity = velocity;
		this.forces = new LinkedList<Vector2D>();
	}
	
	// Methods get
	public Vector2D getPosition() {
		return position;
	}
	
	public Vector2D getVelocity() {
		return velocity;
	}
	
	private Vector2D getAcceleration() {
		return totalForce().dividedBy(mass);
	}
	
	// Methods calculate
	public void calculatePosition() {
		new_position = position.plus(velocity.times(DELTAT));
	}
	
	public void calculateVelocity() {
		new_velocity = velocity.plus(getAcceleration().times(DELTAT));
	}
	
	// Methods update
	public void updatePosition() {
		position = new_position;
	}
	
	public void updateVelocity() {
		velocity = new_velocity;
	}
	
	public void updateParticle() {
		updatePosition();
		updateVelocity();
	}
	
	// Link the particle to the system it belongs
	
	public void setSystem(ParticleSystem system, ArrayList<Particle> particles) {
		this.system = system;
		this.particles = particles;
	}
	
	// FORCES
	
	// Calculate gravitational force on this particle generated by the particle p
	public Vector2D gravitationalForceBy(Particle p) {
		double dist;
		Vector2D force;
		
		// If it's the same particle
		if(p == this)
			return new Vector2D();
					
		// If the particle gets to close
		if (position.distance(p.getPosition()) < r)
			return new Vector2D();
		
		dist = position.distance(p.getPosition());
		force = p.getPosition().minus(position);
		force = force.times(G_CONST * mass * p.mass);
		force = force.dividedBy(Math.pow(dist, 3));
		return force;
	}
	
	// Calculate and sum the total force on this particle generated by the particle p
	public void addForcesBy(Particle p) {
		Vector2D gravitationalForce = gravitationalForceBy(p);
		forces.push(gravitationalForce);
	}
	
	// Add to the forces queue the total forces generated by each other particle
	private void calculateForces() {
		for(Particle p : particles) {
			if(p != this)
				addForcesBy(p);
		}
	}
	
	// Sum all the forces acting in this particle, stored in the Array forces
	private Vector2D totalForce() {
		Vector2D totalForce = new Vector2D();
		while(!forces.isEmpty()) {
			totalForce.add(forces.pop());
		}
		return totalForce;
	}
	
	// Copy the Particle
	@Override
	public Particle clone() {
		return new Particle(mass, position, velocity);
	}
	
	// Generate the Point to print the particle
	public Point point() {
		return new Point((int) Math.round(position.x()), (int) Math.round(position.y()));
	}
	
	// Synchronization barrier so that all threads could calculate their new position and velocity before update them
	private synchronized void waitForOthers() throws InterruptedException{
		if (system.particlesToWait <= 0) {
			notifyAll();
		}
		// Wait while there are particles
		while(system.particlesToWait > 0) {
			wait();
			return;
		}
	}

	@Override
	public void run() {
		// Calculates the new values
		calculateForces();
		calculatePosition();
		calculateVelocity();
		// Waits that all the others particles have done it
		try {
			waitForOthers();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		// Updates itself
		updateParticle();
	}
}
